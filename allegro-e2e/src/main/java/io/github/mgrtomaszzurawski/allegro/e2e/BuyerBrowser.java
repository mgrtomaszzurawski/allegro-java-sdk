/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
 * {@code #login}/{@code #password}). The buy-now / dispute actions are stubs
 * pending a live UI capture (blocked while the probe IP is cooling down).
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
        return !page.url().contains(LOGIN_PATH_SEGMENT);
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
        if (status == HTTP_FORBIDDEN || status == HTTP_TOO_MANY_REQUESTS) {
            // DataDome JS interstitial: sets a cookie and expects a reload.
            page.waitForTimeout(CHALLENGE_SETTLE_MILLIS);
            page.reload(new Page.ReloadOptions().setTimeout(NAV_TIMEOUT_MILLIS));
            page.waitForTimeout(SHORT_SETTLE_MILLIS);
        }
        if (page.content().toLowerCase().contains(BLOCKED_MARKER)) {
            throw new PlaywrightException("DataDome hard-blocked this IP — wait for the block to "
                    + "clear, throttle attempts, and reuse storageState instead of re-logging in");
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
