/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

/**
 * The image formats Allegro accepts for a binary image upload, each carrying the
 * request {@code Content-Type} the upload endpoint requires.
 *
 * @since 0.4.0
 */
public enum ImageFormat {

    /** JPEG image ({@code image/jpeg}). */
    JPEG("image/jpeg"),
    /** PNG image ({@code image/png}). */
    PNG("image/png"),
    /** WebP image ({@code image/webp}). */
    WEBP("image/webp"),
    /** GIF image ({@code image/gif}). */
    GIF("image/gif");

    private final String mediaType;

    ImageFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    /** The HTTP {@code Content-Type} for a binary upload of this format. */
    public String mediaType() {
        return mediaType;
    }
}
