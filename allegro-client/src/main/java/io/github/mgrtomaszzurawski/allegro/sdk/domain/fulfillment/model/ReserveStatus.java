/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Stock-reserve health for a fulfilled product — how comfortable the on-hand
 * quantity is relative to recent sales. Allegro documents this as an open value
 * set that may grow without notice, so an unrecognized token maps to
 * {@link #UNKNOWN} rather than failing the read.
 *
 * @since 0.3.0
 */
public enum ReserveStatus {

    /** Too little sales history to judge the reserve. */
    NOT_ENOUGH_DATA("NOT_ENOUGH_DATA"),

    /** On-hand quantity is running low relative to demand. */
    LOW_STOCK("LOW_STOCK"),

    /** On-hand quantity is healthy. */
    NORMAL("NORMAL"),

    /** More than a year of stock on hand. */
    EXCESS_ONE_YEAR("EXCESS_ONE_YEAR"),

    /** A status Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    ReserveStatus(@Nullable String wireValue) {
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
    public static ReserveStatus fromWire(String wireValue) {
        for (ReserveStatus status : values()) {
            if (wireValue.equals(status.wireValue)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
