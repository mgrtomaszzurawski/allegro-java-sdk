/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceAuthorization;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mints a <strong>buyer</strong> user-context {@link AllegroClient} by automating
 * the OAuth2 device-flow consent in an already-authenticated buyer browser
 * session — the piece that lets buyer-side SDK flows (auction bidding, buyer
 * messaging) run without a human clicking "confirm".
 *
 * <h2>How it works</h2>
 * The SDK's device flow (RFC 8628) calls the {@code userPrompt} callback once,
 * <em>synchronously on the calling thread</em>, with the
 * {@link DeviceAuthorization#verificationUriComplete() one-click verification
 * URL}, then polls the token endpoint until the user confirms. Here the callback
 * drives the {@link BuyerBrowser} page to that URL and confirms: it clicks the
 * consent button when the screen presents one, and for an app the buyer has
 * already authorized it simply lands on the page so the SDK's background poll
 * completes on its own. Because the callback runs on the caller's thread, the same
 * thread that created the Playwright page also drives it — Playwright's thread
 * affinity is respected.
 *
 * <h2>Anti-bot</h2>
 * The consent page ({@code /uzytkownik/bezpieczenstwo/skojarz-aplikacje}) is
 * DataDome-fronted. A JS interstitial clears with the same settle-and-reload as
 * the login page; a full captcha (which a hammered datacenter IP escalates to)
 * does not clear here — mint from a cooled session, or fall back to a one-time
 * manual consent (ARCHITECTURE §10.6). Verified live 2026-07-18: the minted buyer
 * token authenticates as a user distinct from the seller and reaches the bidding
 * API; see {@code BiddingE2ETest}.
 *
 * <h2>Token reuse</h2>
 * The minted refresh token is persisted to the shared {@link BuyerTokenStore}. A
 * later run restores it via {@code ofRefreshToken} and skips the browser
 * entirely — the sustainable path, since it needs no navigation and so never
 * meets DataDome. Only a rejected/expired stored token falls back to the consent
 * step. Allegro rotates the refresh token on every exchange, so the rotated value
 * is re-persisted after each acquisition.
 */
final class BuyerAuthentication {

    /** Candidate selectors for the device-consent confirm button, tried in order.
     * The device-authorization screen has not yet been captured live, so this is
     * a best-effort set (Polish sandbox UI + generic fallbacks); the first visible
     * match is clicked. If none match, the SDK's poll deadline surfaces a clear
     * device-timeout so the real selector can be captured and added here. */
    private static final List<String> CONSENT_BUTTON_SELECTORS = List.of(
            "button[data-role=\"confirm\"]",
            "button:has-text(\"Autoryzuj\")",
            "button:has-text(\"Zezwól\")",
            "button:has-text(\"Potwierdź\")",
            "button:has-text(\"Zaloguj się i pozwól\")",
            "button:has-text(\"Akceptuję\")",
            "button:has-text(\"Tak\")",
            "input[type=\"submit\"]",
            "button[type=\"submit\"]");

    private static final String BLOCKED_MARKER = "zostałeś zablokowany";
    private static final String DATADOME_MARKER = "captcha-delivery.com";
    private static final String CONSENT_DEBUG_DIR_ENV = "E2E_CONSENT_DEBUG_DIR";
    private static final String CONSENT_DEBUG_FILE = "consent-page.html";
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int NAV_TIMEOUT_MILLIS = 45_000;
    // A confirm control, if this screen has one, is present immediately after the
    // page settles — a short probe keeps the no-click path (already-trusted app,
    // where the SDK poll self-completes) from stalling on nine sequential waits.
    private static final int CONSENT_WAIT_MILLIS = 2_500;
    private static final int SETTLE_MILLIS = 4_000;
    private static final int CHALLENGE_SETTLE_MILLIS = 9_000;

    private BuyerAuthentication() {
    }

    /**
     * Return a ready, authenticated buyer client — reusing a stored buyer refresh
     * token when present, otherwise minting one via the browser consent click and
     * persisting it. The returned client is proven live: its {@code me()} login is
     * fetched here before returning.
     *
     * @param browser an already-authenticated buyer browser session (storageState
     *     reused) — used only if a fresh consent click is needed
     * @param clientId sandbox OAuth2 client id
     * @param clientSecret sandbox OAuth2 client secret
     * @return an {@link AllegroClient} bound to the sandbox with a buyer token
     */
    static AllegroClient authenticatedBuyer(BuyerBrowser browser, String clientId,
            String clientSecret) {
        BuyerTokenStore store = new BuyerTokenStore();
        String storedRefreshToken = store.load(BuyerTokenStore.BUYER_ACCOUNT);
        Consumer<DeviceAuthorization> consentPrompt =
                authorization -> approveConsent(browser.page(), authorization.verificationUriComplete());
        DeviceCodeCredentials credentials = storedRefreshToken == null
                ? DeviceCodeCredentials.of(clientId, clientSecret, consentPrompt)
                : DeviceCodeCredentials.ofRefreshToken(clientId, clientSecret, consentPrompt,
                        storedRefreshToken);

        AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX);
        try {
            // Force token acquisition now (runs the consent click if minting) and
            // prove the token works before handing the client back.
            CurrentUser buyer = client.user().me();
            System.out.println("Buyer authenticated: login=" + buyer.login() + ", id=" + buyer.id());
            persistRotated(client, store);
            return client;
        } catch (RuntimeException failure) {
            client.close();
            throw failure;
        }
    }

    /** Re-persist the (rotated) refresh token so the next run can reuse it. */
    private static void persistRotated(AllegroClient client, BuyerTokenStore store) {
        String rotated = client.refreshToken();
        if (rotated != null) {
            store.store(BuyerTokenStore.BUYER_ACCOUNT, rotated);
            System.out.println("Buyer refresh token persisted for reuse.");
        }
    }

    /**
     * Navigate the authenticated buyer page to the device verification URL and
     * click the consent button. Runs on the SDK's calling thread, inside the
     * device-flow {@code userPrompt}.
     */
    private static void approveConsent(Page page, String verificationUriComplete) {
        Response response = page.navigate(verificationUriComplete,
                new Page.NavigateOptions().setTimeout(NAV_TIMEOUT_MILLIS));
        page.waitForTimeout(SETTLE_MILLIS);
        settleDataDomeChallenge(page, response);
        if (page.content().toLowerCase().contains(BLOCKED_MARKER)) {
            throw new PlaywrightException("DataDome hard-blocked this IP on the consent page — wait "
                    + "for the block to clear and reuse the buyer session instead of re-logging in");
        }
        System.out.println("Consent page reached: url=" + page.url() + ", title=" + page.title());
        for (String selector : CONSENT_BUTTON_SELECTORS) {
            Locator button = page.locator(selector).first();
            try {
                button.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(CONSENT_WAIT_MILLIS));
            } catch (PlaywrightException notThisSelector) {
                // Not the confirm control on this screen — try the next candidate.
                continue;
            }
            button.click();
            page.waitForTimeout(SETTLE_MILLIS);
            System.out.println("Device-flow consent confirmed.");
            return;
        }
        // No known confirm control appeared. The buyer may already have authorized
        // this app (the SDK poll will then succeed anyway); if not, the poll
        // deadline surfaces a device-timeout and the live selector must be added.
        // Enumerate the page's actual controls (labels only — no PII) so the real
        // consent selector can be pinned from a single run.
        System.out.println("No consent button matched a known selector. Controls on the page:");
        describeInteractiveControls(page);
        dumpConsentPageForDebug(page);
        System.out.println("Relying on the SDK poll (app may already be authorized).");
    }

    /**
     * Clear a DataDome JS interstitial on the consent page, mirroring the proven
     * {@code BuyerBrowser} recipe: a 403/429 (or a captcha-delivery challenge page)
     * sets a cookie and expects one reload, after which the real page renders. Only
     * a persisting captcha or the hard-block text remains after this.
     */
    private static void settleDataDomeChallenge(Page page, Response response) {
        int status = response == null ? 0 : response.status();
        boolean challenged = status == HTTP_FORBIDDEN || status == HTTP_TOO_MANY_REQUESTS
                || page.content().contains(DATADOME_MARKER);
        if (!challenged) {
            return;
        }
        System.out.println("DataDome challenge on consent page (status " + status
                + ") — settling and reloading once.");
        page.waitForTimeout(CHALLENGE_SETTLE_MILLIS);
        page.reload(new Page.ReloadOptions().setTimeout(NAV_TIMEOUT_MILLIS));
        page.waitForTimeout(SETTLE_MILLIS);
    }

    /** Print each visible clickable control's label — diagnostics for pinning the
     * consent selector. Labels/ids/classes only; never page text or user data. */
    private static void describeInteractiveControls(Page page) {
        System.out.println("  iframes on page: " + page.frames().size());
        List<Locator> controls = page.locator(
                "button, input[type=\"submit\"], input[type=\"button\"], a, [role=\"button\"]").all();
        if (controls.isEmpty()) {
            System.out.println("  (no clickable controls found)");
            return;
        }
        for (Locator control : controls) {
            try {
                if (!control.isVisible()) {
                    continue;
                }
                String text = control.innerText().replace('\n', ' ').trim();
                if (text.isEmpty() && control.getAttribute("value") != null) {
                    text = control.getAttribute("value");
                }
                System.out.println("  control: text='" + text
                        + "', id='" + control.getAttribute("id")
                        + "', class='" + control.getAttribute("class")
                        + "', data-role='" + control.getAttribute("data-role") + "'");
            } catch (PlaywrightException detached) {
                // Control left the DOM between enumeration and inspection — skip it.
                System.out.println("  (control detached during inspection)");
            }
        }
    }

    /** Optional deep diagnostic: when {@code E2E_CONSENT_DEBUG_DIR} is set, save the
     * consent page's HTML there so the exact confirm control can be identified.
     * Off by default; the file may contain page text, so keep the dir outside git. */
    private static void dumpConsentPageForDebug(Page page) {
        String debugDir = System.getenv(CONSENT_DEBUG_DIR_ENV);
        if (debugDir == null) {
            return;
        }
        try {
            Path target = Path.of(debugDir, CONSENT_DEBUG_FILE);
            Files.writeString(target, page.content());
            System.out.println("  consent page HTML saved to " + target);
        } catch (IOException dumpFailure) {
            System.out.println("  consent page HTML dump failed: " + dumpFailure.getMessage());
        }
    }
}
