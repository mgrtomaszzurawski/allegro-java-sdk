/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferBundle;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/offers-extras.md} fixed-bundle snippet —
 * if the documented usage stops compiling, this module breaks the build.
 */
public final class OfferBundlesExample {

    private OfferBundlesExample() {
    }

    static long countBundlesAndUpdateDiscount(String clientId, String clientSecret,
            String bundleId, String marketplaceId) {
        // These endpoints need a user (seller) token — see docs/offers-extras.md.
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            OfferBundles bundles = client.offers().bundles();
            long total = bundles.streamBundles().count();
            OfferBundle updated = bundles.updateDiscount(bundleId,
                    List.of(new BundleDiscount(marketplaceId, Money.of("15.00", "PLN"))));
            System.out.println("updated bundle " + updated.id());
            return total;
        }
    }
}
