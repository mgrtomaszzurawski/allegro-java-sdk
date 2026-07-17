/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;

/**
 * Compile-only twin of the README quick-start snippet — if the README code
 * stops compiling, this module breaks the build.
 */
public final class QuickStartExample {

    private QuickStartExample() {
    }

    static String quickStart(String clientId, String clientSecret) {
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));

        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            var me = client.user().me();
            System.out.println("Logged in as " + me.login());
            return client.refreshToken();
        }
    }
}
