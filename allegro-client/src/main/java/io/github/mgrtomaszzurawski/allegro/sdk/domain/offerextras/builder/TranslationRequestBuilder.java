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
 * Fluent builder for {@link TranslationRequest}. Every part is optional, but
 * {@link #build()} requires at least one part (title, description, or safety
 * information) to be set — an empty update is rejected fail-fast. A blank title
 * and an empty safety-information list are rejected too.
 *
 * @since 0.2.0
 */
public final class TranslationRequestBuilder {

    private static final String ERR_EMPTY =
            "a translation update must set at least one of title, description, or safety information";
    private static final String ERR_TITLE_BLANK = "title must not be blank when set";
    private static final String ERR_DESCRIPTION_EMPTY = "description must have at least one section when set";
    private static final String ERR_SAFETY_EMPTY = "safetyInformation must not be empty when set";

    private @Nullable String title;
    private @Nullable StandardizedDescription description;
    private @Nullable List<ProductSafetyInformationTranslation> safetyInformation;

    /**
     * Set the translated title, or {@code null} to leave it unchanged.
     *
     * @param translatedTitle the translated title
     * @return this builder
     */
    public TranslationRequestBuilder title(@Nullable String translatedTitle) {
        this.title = translatedTitle;
        return this;
    }

    /**
     * Set the translated standardized description, or {@code null} to leave it
     * unchanged.
     *
     * @param translatedDescription the translated description
     * @return this builder
     */
    public TranslationRequestBuilder description(@Nullable StandardizedDescription translatedDescription) {
        this.description = translatedDescription;
        return this;
    }

    /**
     * Set the per-product safety-information translations, or {@code null} to leave
     * them unchanged.
     *
     * @param translations the per-product safety-information translations
     * @return this builder
     */
    public TranslationRequestBuilder safetyInformation(
            @Nullable List<ProductSafetyInformationTranslation> translations) {
        this.safetyInformation = translations == null ? null : List.copyOf(translations);
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if no part is set, the title is blank, or the
     *     safety-information list is empty
     */
    public TranslationRequest build() {
        if (title != null && title.isBlank()) {
            throw new IllegalStateException(ERR_TITLE_BLANK);
        }
        if (description != null && description.sections().isEmpty()) {
            throw new IllegalStateException(ERR_DESCRIPTION_EMPTY);
        }
        if (safetyInformation != null && safetyInformation.isEmpty()) {
            throw new IllegalStateException(ERR_SAFETY_EMPTY);
        }
        if (title == null && description == null && safetyInformation == null) {
            throw new IllegalStateException(ERR_EMPTY);
        }
        return new TranslationRequest(title, description, safetyInformation);
    }
}
