/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

/**
 * One-time buyer-session bootstrap: log in on the sandbox web UI and persist the
 * {@code storageState} (cookies incl. the DataDome cookie + session) so every
 * later E2E run reuses it instead of logging in again — the only sustainable
 * pattern from a datacenter IP, where fresh logins trip DataDome's hard block.
 *
 * <p>Run headed under Xvfb, ideally from a clean IP the first time:
 * <pre>
 *   set -a; . /workspace/shared/secrets/allegro-sandbox.env; set +a
 *   Xvfb :99 -screen 0 1366x900x24 &gt;/tmp/xvfb.log 2&gt;&amp;1 &amp;
 *   DISPLAY=:99 ./gradlew :allegro-e2e:run
 * </pre>
 *
 * Output is status-level only — never credentials or cookies.
 */
public final class BuyerSessionBootstrap {

    private BuyerSessionBootstrap() {
    }

    public static void main(String[] args) {
        BuyerCredentials credentials = BuyerCredentials.fromEnv();
        var storageStatePath = BuyerCredentials.storageStatePath();
        System.out.println("Bootstrapping buyer session → " + storageStatePath);
        try (BuyerBrowser browser = BuyerBrowser.authenticated(credentials, storageStatePath)) {
            // authenticated() already saved the state if a login was performed;
            // save again to refresh cookies after the validity navigation.
            browser.saveState();
            System.out.println("Buyer session ready; storageState persisted. "
                    + "E2E tests will now reuse it without logging in.");
        }
    }
}
