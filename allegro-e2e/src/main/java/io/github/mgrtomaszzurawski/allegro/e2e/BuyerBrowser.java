/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An authenticated buyer session on the Allegro <strong>sandbox</strong> web UI,
 * driven in-process by Playwright's Java binding so a Java E2E test can
 * interleave web-only buyer actions (device-flow consent, buy-now, disputes)
 * with SDK calls and assertions.
 *
 * <h2>Why storage-state reuse is mandatory</h2>
 * Allegro fronts with DataDome. From a datacenter IP, logging in fresh on every
 * run trips a hard IP block ("Zostałeś zablokowany… w tej samej sieci operuje
 * robot"). So this class logs in <em>at most once</em>: it reuses a saved
 * {@code storageState} (cookies incl. the DataDome cookie + session) when one is
 * present and still valid, and only performs the full login — then saves the
 * state — when there is no usable session. Point every E2E run at the same
 * state file and the browser authenticates once, not per test.
 *
 * <h2>Headed under Xvfb</h2>
 * DataDome blocks headless, so this launches full Chromium ({@code headless=false})
 * which needs a display: {@code Xvfb :99 …; DISPLAY=:99}.
 *
 * <p>The login recipe is wire-verified (challenge-settle reload → RODO consent →
 * {@code #login}/{@code #password}) and only ever handles the <em>self-clearing</em>
 * DataDome JS interstitial. When DataDome escalates to its <strong>interactive
 * slider/audio CAPTCHA</strong> (verified 2026-07-18 on the device-flow consent URL
 * from a datacenter IP), this class does <em>not</em> attempt to solve it —
 * automating an anti-bot puzzle would be detection evasion. It fails loudly
 * ({@link #dataDomeChallengePresent()} → {@code ERR_INTERACTIVE_CAPTCHA}) and the
 * token/session is minted once by a human in a normal browser instead
 * (see {@code KNOWN-SERVER-BEHAVIORS.md} and {@code TESTING.md} §3).
 */
public final class BuyerBrowser implements AutoCloseable {

    private static final String SANDBOX_BASE = "https://allegro.pl.allegrosandbox.pl";
    private static final String LOGIN_URL = SANDBOX_BASE + "/logowanie";
    private static final String ACCOUNT_URL = SANDBOX_BASE + "/moje-allegro/moje-dane";
    private static final String LOGIN_PATH_SEGMENT = "/logowanie";
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/149.0.0.0 Safari/537.36";
    private static final String WEBDRIVER_MASK =
            "Object.defineProperty(navigator, 'webdriver', { get: () => false })";
    private static final String CONSENT_SELECTOR = "button[data-role=\"accept-consent\"]";
    private static final String LOGIN_FIELD = "#login";
    private static final String PASSWORD_FIELD = "#password";
    private static final String SUBMIT_SELECTOR = "button[type=\"submit\"]";
    private static final String BLOCKED_MARKER = "zostałeś zablokowany";
    /** DataDome renders its challenges inside an iframe from this host. */
    private static final String CAPTCHA_FRAME_MARKER = "captcha-delivery.com";
    /** Text present on every DataDome challenge page (interstitial and puzzle). */
    private static final String CHALLENGE_TEXT_MARKER = "jesteś człowiekiem";
    private static final String ERR_INTERACTIVE_CAPTCHA =
            "DataDome served an interactive CAPTCHA (slider/audio puzzle) — automated login neither "
                    + "can nor will solve an anti-bot puzzle. Mint the token by opening the device-flow "
                    + "consent URL once in a normal browser (auth-bootstrap); see TESTING.md §3.";

    private static final int NAV_TIMEOUT_MILLIS = 45_000;
    private static final int ACTION_TIMEOUT_MILLIS = 8_000;
    private static final int CHALLENGE_SETTLE_MILLIS = 9_000;
    private static final int SHORT_SETTLE_MILLIS = 4_000;
    private static final int VIEWPORT_WIDTH = 1366;
    private static final int VIEWPORT_HEIGHT = 900;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final Path storageStatePath;

    private BuyerBrowser(Playwright playwright, Browser browser, BrowserContext context,
            Path storageStatePath) {
        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
        this.page = context.newPage();
        this.storageStatePath = storageStatePath;
    }

    /**
     * Open a buyer session, reusing {@code storageStatePath} when it holds a
     * still-valid session and logging in (then saving state) only when needed.
     */
    public static BuyerBrowser authenticated(BuyerCredentials credentials, Path storageStatePath) {
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage",
                        "--disable-blink-features=AutomationControlled")));
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setLocale("pl-PL")
                .setTimezoneId("Europe/Warsaw")
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        if (Files.exists(storageStatePath)) {
            contextOptions.setStorageStatePath(storageStatePath);
        }
        BrowserContext context = browser.newContext(contextOptions);
        context.addInitScript(WEBDRIVER_MASK);
        BuyerBrowser buyerBrowser = new BuyerBrowser(playwright, browser, context, storageStatePath);
        try {
            if (!buyerBrowser.hasValidSession()) {
                buyerBrowser.logIn(credentials);
                buyerBrowser.saveState();
            }
            return buyerBrowser;
        } catch (RuntimeException failure) {
            // Close the just-launched browser + driver so a failed login (e.g. the
            // DataDome hard block, which throws) never leaks an orphaned process.
            buyerBrowser.close();
            throw failure;
        }
    }

    /** {@code true} if the current context is already an authenticated buyer. */
    public boolean hasValidSession() {
        page.navigate(ACCOUNT_URL, new Page.NavigateOptions().setTimeout(NAV_TIMEOUT_MILLIS));
        page.waitForTimeout(SHORT_SETTLE_MILLIS);
        // DataDome serves its challenge AT the destination URL, so a bare
        // "not on /logowanie" check false-positives on a challenge page (it
        // reports a stale/blocked session as valid). Only a challenge-free page
        // that was not redirected to login counts as an authenticated session.
        if (dataDomeChallengePresent()) {
            return false;
        }
        return !page.url().contains(LOGIN_PATH_SEGMENT);
    }

    /**
     * {@code true} while any DataDome challenge is on screen — the JS
     * interstitial (self-clears on reload) or the interactive slider/audio
     * CAPTCHA (does not). The challenge widget lives in a
     * {@value #CAPTCHA_FRAME_MARKER} iframe; the host page carries the marker text.
     */
    private boolean dataDomeChallengePresent() {
        List<String> frameUrls = new ArrayList<>();
        for (Frame frame : page.frames()) {
            frameUrls.add(frame.url());
        }
        return isDataDomeChallenge(page.content(), frameUrls);
    }

    /**
     * Pure predicate behind {@link #dataDomeChallengePresent()}, split out so the
     * detection can be unit-tested without a live browser or the sandbox: a
     * DataDome challenge is present when any frame comes from the captcha host or
     * the host page carries the challenge marker text.
     *
     * @param pageContent host-page HTML (any case)
     * @param frameUrls URLs of every frame on the page
     */
    static boolean isDataDomeChallenge(String pageContent, List<String> frameUrls) {
        for (String frameUrl : frameUrls) {
            if (frameUrl.contains(CAPTCHA_FRAME_MARKER)) {
                return true;
            }
        }
        return pageContent.toLowerCase(Locale.ROOT).contains(CHALLENGE_TEXT_MARKER);
    }

    /**
     * Perform the full DataDome-aware login. Call sparingly — every fresh login
     * from a datacenter IP risks the hard block.
     */
    private void logIn(BuyerCredentials credentials) {
        Response response = page.navigate(LOGIN_URL,
                new Page.NavigateOptions().setTimeout(NAV_TIMEOUT_MILLIS));
        int status = response == null ? 0 : response.status();
        page.waitForTimeout(SHORT_SETTLE_MILLIS);
        if (status == HTTP_FORBIDDEN || status == HTTP_TOO_MANY_REQUESTS
                || dataDomeChallengePresent()) {
            // DataDome JS interstitial: sets a cookie and expects a reload to
            // self-clear. (A status check alone misses challenges served on 200.)
            page.waitForTimeout(CHALLENGE_SETTLE_MILLIS);
            page.reload(new Page.ReloadOptions().setTimeout(NAV_TIMEOUT_MILLIS));
            page.waitForTimeout(SHORT_SETTLE_MILLIS);
        }
        String content = page.content().toLowerCase(Locale.ROOT);
        if (content.contains(BLOCKED_MARKER)) {
            throw new PlaywrightException("DataDome hard-blocked this IP — wait for the block to "
                    + "clear, throttle attempts, and reuse storageState instead of re-logging in");
        }
        if (dataDomeChallengePresent()) {
            // The interstitial did not self-clear: this is the interactive puzzle
            // tier, which we do not solve (anti-bot evasion). Fail loudly.
            throw new PlaywrightException(ERR_INTERACTIVE_CAPTCHA);
        }
        dismissConsent();
        Locator loginField = page.locator(LOGIN_FIELD);
        loginField.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(ACTION_TIMEOUT_MILLIS));
        loginField.fill(credentials.login());
        page.locator(PASSWORD_FIELD).fill(credentials.password());
        page.locator(SUBMIT_SELECTOR).first().click();
        page.waitForTimeout(CHALLENGE_SETTLE_MILLIS);
        if (page.url().replaceFirst("\\?.*", "").endsWith(LOGIN_PATH_SEGMENT)) {
            throw new PlaywrightException("Login did not complete — still on the login page");
        }
    }

    /** Dismiss the RODO/cookie consent modal if present; never abort on absence. */
    private void dismissConsent() {
        try {
            Locator consent = page.locator(CONSENT_SELECTOR).first();
            consent.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(ACTION_TIMEOUT_MILLIS));
            consent.click();
            consent.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(ACTION_TIMEOUT_MILLIS));
        } catch (PlaywrightException ignored) {
            // No consent modal on this session — the form is directly usable.
        }
    }

    /** Persist cookies + session so later runs skip the login (and the challenge). */
    public void saveState() {
        context.storageState(new BrowserContext.StorageStateOptions().setPath(storageStatePath));
        // The state holds live session cookies — restrict it to the owner, like
        // every other file under the shared secrets dir.
        try {
            Files.setPosixFilePermissions(storageStatePath,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (IOException e) {
            throw new PlaywrightException("Failed to restrict storageState permissions: "
                    + e.getMessage());
        }
    }

    /** The live page, for a test that needs to drive the UI directly. */
    public Page page() {
        return page;
    }

    @Override
    public void close() {
        try {
            browser.close();
        } finally {
            // Always release the driver process even if closing the browser threw.
            playwright.close();
        }
    }
}
