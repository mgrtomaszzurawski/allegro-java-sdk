/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

/**
 * The kind of content a {@link DescriptionSectionItem} carries within a
 * standardized offer description.
 *
 * @since 0.2.0
 */
public enum DescriptionItemType {

    /** A block of text (the item's {@code content}). */
    TEXT,

    /** An image (the item's {@code url}). */
    IMAGE,

    /** An item kind Allegro introduced that this SDK version does not model yet. */
    UNKNOWN
}
