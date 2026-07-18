/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;

/**
 * Compile-only twin of the {@code docs/offers-extras.md} flexible-bundle snippet —
 * if the documented usage stops compiling, this module breaks the build.
 */
public final class FlexibleBundlesExample {

    private FlexibleBundlesExample() {
    }

    static int countSlots(String clientId, String clientSecret, String bundleId) {
        // These endpoints need a user (seller) token — see docs/offers-extras.md.
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            FlexibleBundles flexible = client.offers().flexibleBundles();
            long total = flexible.streamBundles().count();
            System.out.println(total + " flexible bundle(s)");
            FlexibleBundle bundle = flexible.get(bundleId);
            return bundle.slots().size();
        }
    }
}
