/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * The disposition the warehouse assigned to a received quantity of a product.
 * Allegro documents this as an open value set that may grow without notice, so
 * an unrecognized token maps to {@link #UNKNOWN} rather than failing the read.
 *
 * @since 0.4.0
 */
public enum ReceivedType {

    /** Received in sellable condition and added to stock. */
    SELLABLE("SELLABLE"),

    /** Received damaged. */
    DAMAGE("DAMAGE"),

    /** Rejected on receipt. */
    REJECT("REJECT"),

    /** A type Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    ReceivedType(@Nullable String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact token Allegro uses on the wire, or {@code null} for {@link #UNKNOWN}. */
    public @Nullable String wireValue() {
        return wireValue;
    }

    /**
     * Resolve the enum from the wire token. The set is open, so an unrecognized
     * token maps to {@link #UNKNOWN} instead of throwing.
     */
    public static ReceivedType fromWire(String wireValue) {
        for (ReceivedType type : values()) {
            if (wireValue.equals(type.wireValue)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
