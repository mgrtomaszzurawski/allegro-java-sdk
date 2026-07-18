/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/offers-extras.md} translation and rating
 * snippets — if the documented usage stops compiling, this module breaks the
 * build.
 */
public final class OfferTranslationsExample {

    private OfferTranslationsExample() {
    }

    static int translateTitleAndReadRating(String clientId, String clientSecret,
            String offerId, String language, String title) {
        // These endpoints need a user (seller) token — see docs/offers-extras.md.
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            client.offers().translations()
                    .update(offerId, language, TranslationRequest.builder().title(title).build());
            List<OfferTranslation> translations = client.offers().translations().ofOffer(offerId);
            for (OfferTranslation translation : translations) {
                System.out.println(translation.language() + ": " + translation.title());
            }
            OfferRating rating = client.offers().rating(offerId);
            return rating.totalResponses();
        }
    }
}
