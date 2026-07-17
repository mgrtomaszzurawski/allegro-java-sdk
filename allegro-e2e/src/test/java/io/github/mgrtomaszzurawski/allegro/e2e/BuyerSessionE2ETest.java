/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live buyer-session E2E — the pattern every web-dependent E2E test follows:
 * open a Playwright buyer session (reusing the saved {@code storageState}) and,
 * from there, interleave web-only buyer actions with SDK calls and assertions.
 *
 * <p><strong>Not part of {@code check}.</strong> These tests need a real browser
 * under Xvfb, the live sandbox, and buyer credentials, so they run only with
 * {@code -Pe2e} (see the module build). A bootstrapped storageState must already
 * exist (run {@code :allegro-e2e:run} once) — otherwise the test logs in, which
 * from a datacenter IP risks DataDome's block.
 *
 * <pre>
 *   set -a; . /workspace/shared/secrets/allegro-sandbox.env; set +a
 *   Xvfb :99 -screen 0 1366x900x24 &gt;/tmp/xvfb.log 2&gt;&amp;1 &amp;
 *   DISPLAY=:99 ./gradlew :allegro-e2e:test -Pe2e
 * </pre>
 */
@Tag("e2e")
class BuyerSessionE2ETest {

    private static final String LOGIN_PATH_SEGMENT = "/logowanie";

    @Test
    void authenticated_whenStorageStateExists_reusesSessionWithoutLogin() {
        // given — a bootstrapped buyer session (assert the precondition explicitly
        // so a missing storageState fails loudly rather than silently logging in)
        var storageStatePath = BuyerCredentials.storageStatePath();
        assertTrue(Files.exists(storageStatePath),
                "run :allegro-e2e:run once to bootstrap the buyer storageState first");

        // when — open the session
        try (BuyerBrowser browser = BuyerBrowser.authenticated(
                BuyerCredentials.fromEnv(), storageStatePath)) {

            // then — the reused session is already authenticated (not bounced to login)
            assertTrue(browser.hasValidSession());
            assertFalse(browser.page().url().contains(LOGIN_PATH_SEGMENT));
        }
    }
}
