/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleOfferRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleSlotRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.MarketplaceDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.WholeBundleDiscount;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/offers-extras.md} flexible-bundle snippet —
 * if the documented usage stops compiling, this module breaks the build.
 */
public final class FlexibleBundlesExample {

    private static final int MINIMUM_BOUGHT_OFFERS = 2;
    private static final int WHOLE_BUNDLE_PERCENTAGE = 10;

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

    static String createTwoSlotBundle(String clientId, String clientSecret,
            String phoneOfferId, String caseOfferId) {
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            FlexibleBundle created = client.offers().flexibleBundles().create(FlexibleBundleRequest.builder()
                    .slot(FlexibleBundleSlotRequest.builder()
                            .order(0).entryPoint(true).requiredQuantity(1)
                            .offer(FlexibleBundleOfferRef.of(phoneOfferId, false))
                            .build())
                    .slot(FlexibleBundleSlotRequest.builder()
                            .order(1).requiredQuantity(1)
                            .offer(FlexibleBundleOfferRef.of(caseOfferId, false))
                            .build())
                    .discount(FlexibleBundleDiscount.wholeBundle(new WholeBundleDiscount(
                            MINIMUM_BOUGHT_OFFERS,
                            List.of(new MarketplaceDiscount("allegro-pl", WHOLE_BUNDLE_PERCENTAGE)))))
                    .build());
            return created.id();
        }
    }
}
