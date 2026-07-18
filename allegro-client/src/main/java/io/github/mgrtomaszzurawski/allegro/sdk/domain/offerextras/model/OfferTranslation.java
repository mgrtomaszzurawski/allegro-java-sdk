/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferTitleTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationTypeRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer's translation into one language, as returned by
 * {@code OfferTranslations.ofOffer(String)}. The SDK currently exposes the
 * <strong>title</strong> translation; the (richly structured) description and
 * safety-information translations are not yet modelled.
 *
 * @param language the BCP-47 language tag (for example {@code en-US})
 * @param title the translated title, or {@code null} when the offer has no
 *     title translation for this language
 * @param titleType how the title translation was produced, or {@code null} when
 *     there is no title translation
 *
 * @since 0.2.0
 */
public record OfferTranslation(
        String language,
        @Nullable String title,
        @Nullable OfferTranslationType titleType) {

    /** Map one generated Layer-1 translation DTO to the public record. */
    static OfferTranslation from(OfferTranslationRaw raw) {
        OfferTitleTranslationRaw titleRaw = raw.getTitle();
        return new OfferTranslation(
                raw.getLanguage(),
                titleRaw == null ? null : titleRaw.getTranslation(),
                titleRaw == null ? null : typeFrom(titleRaw.getType()));
    }

    /** Map the generated Layer-1 list response to public records. */
    public static List<OfferTranslation> listFrom(OfferTranslationsRaw raw) {
        return raw.getTranslations() == null
                ? List.of()
                : raw.getTranslations().stream().map(OfferTranslation::from).toList();
    }

    private static @Nullable OfferTranslationType typeFrom(@Nullable OfferTranslationTypeRaw raw) {
        if (raw == null) {
            return null;
        }
        try {
            return OfferTranslationType.valueOf(raw.name());
        } catch (IllegalArgumentException unknownType) {
            // A title-type Allegro added after this SDK version — degrade rather
            // than fail the whole translations read.
            return OfferTranslationType.UNKNOWN;
        }
    }
}
