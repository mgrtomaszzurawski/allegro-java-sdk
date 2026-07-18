/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import org.jspecify.annotations.Nullable;

/**
 * A tag to create or rename. Build it with {@link #builder()}, which validates
 * the required name fail-fast.
 *
 * @param name the tag name (required)
 * @param hidden whether the tag is hidden from the seller's own tag views, or
 *     {@code null} to leave it to the server default
 *
 * @since 0.2.0
 */
public record TagRequest(String name, @Nullable Boolean hidden) {

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link TagRequestBuilder}
     */
    public static TagRequestBuilder builder() {
        return new TagRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's fields.
     *
     * @return a builder holding this request's values
     */
    public TagRequestBuilder toBuilder() {
        return new TagRequestBuilder().name(name).hidden(hidden);
    }
}
