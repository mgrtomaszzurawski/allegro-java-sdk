/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw.InputTypeEnum;
import org.jspecify.annotations.Nullable;

/**
 * Whether a compatibility entry is keyed by a product identifier or supplied as
 * free text — the shared {@code ID}/{@code TEXT} axis that describes both how a
 * {@link CompatibleCategory} accepts its compatibility-list items and the kind of
 * an individual {@link CompatibilityItem} on a read.
 *
 * @since 0.2.0
 */
public enum CompatibilityInputType {

    /** Chosen by product identifier from Allegro's compatible-products list. */
    ID,

    /** Entered as free text, bounded by the category's validation rules. */
    TEXT,

    /**
     * An input type this SDK release does not model yet — also the mapping for a
     * category that carries no input type (read-only forward-compat sentinel).
     */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public domain enum. */
    static CompatibilityInputType from(@Nullable InputTypeEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case ID -> ID;
            case TEXT -> TEXT;
            default -> UNKNOWN;
        };
    }
}
