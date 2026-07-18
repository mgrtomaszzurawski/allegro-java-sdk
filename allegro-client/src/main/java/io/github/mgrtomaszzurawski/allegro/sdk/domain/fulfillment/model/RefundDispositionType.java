/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Why goods came back into the warehouse on a refund-disposition report — a
 * buyer return or a delivery that bounced. Allegro documents this as an open
 * value set, so an unrecognized token maps to {@link #UNKNOWN}.
 *
 * @since 0.3.0
 */
public enum RefundDispositionType {

    /** Goods returned by the buyer. */
    RETURN("RETURN"),

    /** Goods that bounced back (undeliverable). */
    BOUNCE("BOUNCE"),

    /** A type Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    RefundDispositionType(@Nullable String wireValue) {
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
    public static RefundDispositionType fromWire(String wireValue) {
        for (RefundDispositionType type : values()) {
            if (wireValue.equals(type.wireValue)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
