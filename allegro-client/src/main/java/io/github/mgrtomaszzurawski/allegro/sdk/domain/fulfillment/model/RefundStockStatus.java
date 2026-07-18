/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * The state returned goods were found in when checked back into the warehouse.
 * Allegro documents this as an open value set, so an unrecognized token maps to
 * {@link #UNKNOWN}.
 *
 * @since 0.3.0
 */
public enum RefundStockStatus {

    /** Goods can be sold again. */
    SELLABLE("SELLABLE"),

    /** Goods cannot be sold (damaged or otherwise unsellable). */
    NON_SELLABLE("NON_SELLABLE"),

    /** Expected goods did not arrive. */
    MISSING("MISSING"),

    /** The returned item did not match what was expected. */
    ITEM_MISMATCH("ITEM_MISMATCH"),

    /** A status Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    RefundStockStatus(@Nullable String wireValue) {
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
    public static RefundStockStatus fromWire(String wireValue) {
        for (RefundStockStatus status : values()) {
            if (wireValue.equals(status.wireValue)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
