/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

/**
 * An offer translation to set for one language. The SDK currently supports the
 * <strong>title</strong> translation; description and safety-information
 * translations are not yet modelled. Build it with {@link #builder()}, which
 * validates the required title fail-fast.
 *
 * @param title the translated title (required)
 *
 * @since 0.2.0
 */
public record TranslationRequest(String title) {

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
        return new TranslationRequestBuilder().title(title);
    }
}
