/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.ProductSafetyInformationTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.StandardizedDescription;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer translation to set for one language: any combination of the title, the
 * standardized description, and the per-product safety information. Only the parts
 * that are set are sent — the update is a partial (PATCH) that leaves the other
 * parts untouched. Build it with {@link #builder()}, which requires at least one
 * part to be set fail-fast.
 *
 * @param title the translated title, or {@code null} to leave it unchanged
 * @param description the translated standardized description, or {@code null} to
 *     leave it unchanged
 * @param safetyInformation the per-product safety-information translations, or
 *     {@code null} to leave them unchanged; never empty when non-null
 *
 * @since 0.2.0
 */
public record TranslationRequest(
        @Nullable String title,
        @Nullable StandardizedDescription description,
        @Nullable List<ProductSafetyInformationTranslation> safetyInformation) {

    public TranslationRequest {
        safetyInformation = safetyInformation == null ? null : List.copyOf(safetyInformation);
    }

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link TranslationRequestBuilder}
     */
    public static TranslationRequestBuilder builder() {
        return new TranslationRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's fields.
     *
     * @return a builder holding this request's values
     */
    public TranslationRequestBuilder toBuilder() {
        return new TranslationRequestBuilder()
                .title(title)
                .description(description)
                .safetyInformation(safetyInformation);
    }
}
