/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import org.jspecify.annotations.Nullable;

/**
 * The kind of a description section item.
 *
 * <p>On the read side the kind is resolved from the deserialized polymorphic
 * subtype (see {@link DescriptionItem#from}); an item kind Allegro adds after this
 * release resolves to {@link #UNKNOWN}.
 *
 * @since 0.3.0
 */
public enum DescriptionItemType {

    /** A block of formatted (HTML) text. */
    TEXT,
    /** An image, referenced by URL. */
    IMAGE,
    /** An item kind this SDK release does not model yet (read-only, never written). */
    UNKNOWN;

    /** The wire discriminator for a text item. */
    static final String WIRE_TEXT = "TEXT";
    /** The wire discriminator for an image item. */
    static final String WIRE_IMAGE = "IMAGE";

    /**
     * Resolve the kind from the wire {@code type} discriminator, tolerating a kind this
     * release does not model (which maps to {@link #UNKNOWN}).
     *
     * @param wireType the wire discriminator token (e.g. {@code TEXT}), or {@code null}
     * @return the matching kind, or {@link #UNKNOWN}
     */
    static DescriptionItemType from(@Nullable String wireType) {
        if (WIRE_TEXT.equals(wireType)) {
            return TEXT;
        }
        if (WIRE_IMAGE.equals(wireType)) {
            return IMAGE;
        }
        return UNKNOWN;
    }
}
