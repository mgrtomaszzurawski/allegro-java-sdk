/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTags;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/offers-extras.md} offer-tags snippet — if
 * the documented usage stops compiling, this module breaks the build.
 */
public final class OfferTagsExample {

    private OfferTagsExample() {
    }

    static long tagAndCount(String clientId, String clientSecret, String offerId) {
        // Tags need a user (seller) token — see docs/offers-extras.md.
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            OfferTags tags = client.offers().tags();
            String tagId = tags.create(TagRequest.builder().name("Priority").build());
            tags.assignToOffer(offerId, List.of(tagId));
            return tags.streamTags().count();
        }
    }
}
