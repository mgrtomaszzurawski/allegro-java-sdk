/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import org.jspecify.annotations.Nullable;

/**
 * The kind of a {@link CategoryEvent}: what happened to the category. The constant
 * names are Allegro's wire discriminator values.
 *
 * @since 0.2.0
 */
public enum CategoryEventType {

    /** A new category was created. */
    CATEGORY_CREATED,

    /** A category was deleted (its offers redirect to another category). */
    CATEGORY_DELETED,

    /** A category was moved to a different parent. */
    CATEGORY_MOVED,

    /** A category was renamed. */
    CATEGORY_RENAMED,

    /**
     * An event type this SDK release does not model yet — a forward-compat sentinel
     * for a type Allegro introduced after this version.
     */
    UNKNOWN;

    /** Map Allegro's wire {@code type} discriminator to the domain enum. */
    static CategoryEventType from(@Nullable String wireType) {
        if (wireType == null) {
            return UNKNOWN;
        }
        for (CategoryEventType candidate : values()) {
            if (candidate != UNKNOWN && candidate.name().equals(wireType)) {
                return candidate;
            }
        }
        return UNKNOWN;
    }

    /**
     * This type's wire discriminator value for a query filter, or {@code null} for
     * {@link #UNKNOWN} (which cannot be sent as a filter).
     *
     * @return the wire value, or {@code null} for {@code UNKNOWN}
     */
    public @Nullable String wireValue() {
        return this == UNKNOWN ? null : name();
    }
}
