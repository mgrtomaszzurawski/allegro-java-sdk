/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import org.jspecify.annotations.Nullable;

/**
 * The kind of a {@link CategoryParameterScheduledChange}. The constant names are
 * Allegro's wire discriminator values.
 *
 * @since 0.2.0
 */
public enum ScheduledChangeType {

    /** A parameter's requirement (whether it is required) will change. */
    REQUIREMENT_CHANGE,

    /**
     * A change type this SDK release does not model yet — a forward-compat sentinel
     * for a type Allegro introduced after this version.
     */
    UNKNOWN;

    /** Map Allegro's wire {@code type} discriminator to the domain enum. */
    static ScheduledChangeType from(@Nullable String wireType) {
        if (wireType == null) {
            return UNKNOWN;
        }
        for (ScheduledChangeType candidate : values()) {
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
