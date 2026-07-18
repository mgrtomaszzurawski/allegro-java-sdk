/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductSafetyInformationDescriptionRaw;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A product's safety-information translation for one language: the translated
 * text keyed by the product this offer sells. Build one for a write with {@link
 * #of(String, String)}; on a read {@code type} reports how the translation was
 * produced.
 *
 * @param productId the identifier of the product the translation applies to;
 *     required for a write ({@link #of(String, String)} enforces it), and
 *     {@code null} on a read only if the server omits it (the field is optional
 *     in the API)
 * @param translation the translated safety-information text, or {@code null} when
 *     the product has no translation for this language
 * @param type how the translation was produced, or {@code null} on a write request
 *     and when there is no translation
 *
 * @since 0.2.0
 */
public record ProductSafetyInformationTranslation(
        @Nullable String productId,
        @Nullable String translation,
        @Nullable OfferTranslationType type) {

    private static final String ERR_PRODUCT_ID_NULL = "productId must not be null";
    private static final String ERR_TRANSLATION_NULL = "translation must not be null";
    private static final String ERR_PRODUCT_ID_NOT_UUID = "productId must be a UUID: ";

    /**
     * A safety-information translation to set for one product. Validates the
     * inputs fail-fast: both are required and {@code productId} must be a UUID
     * (Allegro product identifiers are UUIDs).
     *
     * @param productId the identifier of the product the translation applies to
     * @param translation the translated safety-information text
     * @return a safety-information translation
     * @throws IllegalArgumentException if {@code productId} is not a UUID
     */
    public static ProductSafetyInformationTranslation of(String productId, String translation) {
        Objects.requireNonNull(productId, ERR_PRODUCT_ID_NULL);
        Objects.requireNonNull(translation, ERR_TRANSLATION_NULL);
        try {
            UUID.fromString(productId);
        } catch (IllegalArgumentException notUuid) {
            throw new IllegalArgumentException(ERR_PRODUCT_ID_NOT_UUID + productId, notUuid);
        }
        return new ProductSafetyInformationTranslation(productId, translation, null);
    }

    /** Map one generated Layer-1 safety-information product entry to the public record. */
    static ProductSafetyInformationTranslation from(ProductSafetyInformationDescriptionRaw raw) {
        return new ProductSafetyInformationTranslation(
                Objects.toString(raw.getId(), null),
                raw.getTranslation(),
                OfferTranslationType.fromRaw(raw.getType()));
    }
}
