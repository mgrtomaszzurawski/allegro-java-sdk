/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link TranslationRequest}. The title is required.
 * {@link #build()} validates it fail-fast.
 *
 * @since 0.2.0
 */
public final class TranslationRequestBuilder {

    private static final String ERR_TITLE_REQUIRED = "title is required";

    private @Nullable String title;

    /**
     * Set the translated title (required).
     *
     * @param translatedTitle the translated title
     * @return this builder
     */
    public TranslationRequestBuilder title(String translatedTitle) {
        this.title = translatedTitle;
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if the title is missing or blank
     */
    public TranslationRequest build() {
        if (title == null || title.isBlank()) {
            throw new IllegalStateException(ERR_TITLE_REQUIRED);
        }
        return new TranslationRequest(title);
    }
}
