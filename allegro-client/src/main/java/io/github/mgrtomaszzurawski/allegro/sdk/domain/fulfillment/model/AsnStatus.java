/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Lifecycle state of an Advance Ship Notice, from a local draft through its
 * arrival and unpacking at the warehouse. Allegro documents this as an open
 * value set that may grow without notice, so an unrecognized token maps to
 * {@link #UNKNOWN} rather than failing the read.
 *
 * @since 0.4.0
 */
public enum AsnStatus {

    /** Editable draft that has not yet been submitted to the warehouse. */
    DRAFT("DRAFT"),

    /** Submitted and on its way to the warehouse. */
    IN_TRANSIT("IN_TRANSIT"),

    /** Arrived and being unpacked and received at the warehouse. */
    UNPACKING("UNPACKING"),

    /** Fully received; the notice is closed. */
    COMPLETED("COMPLETED"),

    /** Cancelled before completion. */
    CANCELLED("CANCELLED"),

    /** A status Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    AsnStatus(@Nullable String wireValue) {
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
    public static AsnStatus fromWire(String wireValue) {
        for (AsnStatus status : values()) {
            if (wireValue.equals(status.wireValue)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
