/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSectionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.ProductSafetyInformationTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.StandardizedDescription;
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
            String offerId, String language, String title, String productId) {
        // These endpoints need a user (seller) token — see docs/offers-extras.md.
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            // A partial update: title, description, and safety information in one call;
            // parts left unset would be untouched.
            client.offers().translations().update(offerId, language, TranslationRequest.builder()
                    .title(title)
                    .description(StandardizedDescription.of(DescriptionSection.of(
                            DescriptionSectionItem.text("A comfortable, quiet wireless keyboard."),
                            DescriptionSectionItem.image("https://images.allegrostatic.pl/kbd.jpg"))))
                    .safetyInformation(List.of(
                            ProductSafetyInformationTranslation.of(productId, "Keep away from water.")))
                    .build());
            List<OfferTranslation> translations = client.offers().translations().ofOffer(offerId);
            for (OfferTranslation translation : translations) {
                System.out.println(translation.language() + ": " + translation.title()
                        + " (" + descriptionSectionCount(translation) + " description section(s), "
                        + translation.safetyInformation().size() + " safety product(s))");
            }
            OfferRating rating = client.offers().rating(offerId);
            return rating.totalResponses();
        }
    }

    private static int descriptionSectionCount(OfferTranslation translation) {
        StandardizedDescription description = translation.description();
        return description == null ? 0 : description.sections().size();
    }
}
