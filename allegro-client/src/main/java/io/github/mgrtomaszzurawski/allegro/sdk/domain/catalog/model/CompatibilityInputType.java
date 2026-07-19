/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw.InputTypeEnum;
import org.jspecify.annotations.Nullable;

/**
 * How a {@link CompatibleCategory}'s compatibility-list items are supplied when
 * building an offer in that category.
 *
 * @since 0.2.0
 */
public enum CompatibilityInputType {

    /** Items are chosen by product identifier from Allegro's compatible-products list. */
    ID,

    /** Items are entered as free text, bounded by the category's validation rules. */
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
