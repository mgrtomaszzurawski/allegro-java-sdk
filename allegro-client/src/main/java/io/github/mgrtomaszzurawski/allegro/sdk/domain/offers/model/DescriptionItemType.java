/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import org.jspecify.annotations.Nullable;

/**
 * The kind of a description section item.
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

    /** Map the generated {@code type} discriminator, tolerating unknown future values. */
    public static DescriptionItemType from(@Nullable String wire) {
        if (WIRE_TEXT.equals(wire)) {
            return TEXT;
        }
        if (WIRE_IMAGE.equals(wire)) {
            return IMAGE;
        }
        return UNKNOWN;
    }
}
