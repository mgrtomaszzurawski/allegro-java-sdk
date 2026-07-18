/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link TagRequest}. The name is required; the hidden flag is
 * optional. {@link #build()} validates the required name fail-fast.
 *
 * @since 0.2.0
 */
public final class TagRequestBuilder {

    private static final String ERR_NAME_REQUIRED = "name is required";

    private @Nullable String name;
    private @Nullable Boolean hidden;

    /**
     * Set the tag name (required).
     *
     * @param tagName the tag name
     * @return this builder
     */
    public TagRequestBuilder name(String tagName) {
        this.name = tagName;
        return this;
    }

    /**
     * Set whether the tag is hidden from the seller's own tag views (optional).
     *
     * @param tagHidden the hidden flag, or {@code null} for the server default
     * @return this builder
     */
    public TagRequestBuilder hidden(@Nullable Boolean tagHidden) {
        this.hidden = tagHidden;
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if the name is missing or blank
     */
    public TagRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        return new TagRequest(name, hidden);
    }
}
