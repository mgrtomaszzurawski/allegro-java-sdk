/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferDescriptionTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferSafetyInformationTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTitleTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationsRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer's translation into one language, as returned by
 * {@code OfferTranslations.ofOffer(String)}. Covers the title, the standardized
 * description, and the per-product safety-information translations; each carries
 * how it was produced (its {@link OfferTranslationType}).
 *
 * @param language the BCP-47 language tag (for example {@code en-US})
 * @param title the translated title, or {@code null} when the offer has no title
 *     translation for this language
 * @param titleType how the title translation was produced, or {@code null} when
 *     there is no title translation
 * @param description the translated standardized description, or {@code null} when
 *     the offer has no description translation for this language
 * @param descriptionType how the description translation was produced, or {@code
 *     null} when there is no description translation
 * @param safetyInformation the per-product safety-information translations; never
 *     {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record OfferTranslation(
        String language,
        @Nullable String title,
        @Nullable OfferTranslationType titleType,
        @Nullable StandardizedDescription description,
        @Nullable OfferTranslationType descriptionType,
        List<ProductSafetyInformationTranslation> safetyInformation) {

    public OfferTranslation {
        safetyInformation = List.copyOf(safetyInformation);
    }

    /** Map one generated Layer-1 translation DTO to the public record. */
    static OfferTranslation from(OfferTranslationRaw raw) {
        OfferTitleTranslationRaw titleRaw = raw.getTitle();
        OfferDescriptionTranslationRaw descriptionRaw = raw.getDescription();
        return new OfferTranslation(
                raw.getLanguage(),
                titleRaw == null ? null : titleRaw.getTranslation(),
                titleRaw == null ? null : OfferTranslationType.fromRaw(titleRaw.getType()),
                descriptionFrom(descriptionRaw),
                descriptionRaw == null ? null : OfferTranslationType.fromRaw(descriptionRaw.getType()),
                safetyFrom(raw.getSafetyInformation()));
    }

    /** Map the generated Layer-1 list response to public records. */
    public static List<OfferTranslation> listFrom(OfferTranslationsRaw raw) {
        return raw.getTranslations() == null
                ? List.of()
                : raw.getTranslations().stream().map(OfferTranslation::from).toList();
    }

    private static @Nullable StandardizedDescription descriptionFrom(@Nullable OfferDescriptionTranslationRaw raw) {
        return raw == null || raw.getTranslation() == null
                ? null
                : StandardizedDescription.from(raw.getTranslation());
    }

    private static List<ProductSafetyInformationTranslation> safetyFrom(
            @Nullable OfferSafetyInformationTranslationRaw raw) {
        return raw == null || raw.getProducts() == null
                ? List.of()
                : raw.getProducts().stream().map(ProductSafetyInformationTranslation::from).toList();
    }
}
