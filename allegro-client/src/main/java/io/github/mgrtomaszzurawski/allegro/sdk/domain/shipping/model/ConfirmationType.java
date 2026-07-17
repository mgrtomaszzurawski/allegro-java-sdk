/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * How the seller confirms a pickup at a point of service. {@link #UNKNOWN} is a
 * read-only sentinel for a server value this SDK release does not yet model.
 *
 * @since 0.2.0
 */
public enum ConfirmationType {

    /** The seller informs the buyer when the parcel is ready for collection. */
    AWAIT_CONTACT,

    /** The buyer must arrange a collection time with the seller. */
    CALL_US,

    /** No contact is required before collection. */
    CONTACT_NOT_REQUIRED,

    /** A value returned by the server that this SDK release does not model. */
    UNKNOWN;

    private static final String ERR_UNKNOWN =
            "UNKNOWN is a read-only sentinel and cannot be sent to Allegro";

    /**
     * Wire representation to send to Allegro.
     *
     * @throws IllegalStateException if called on {@link #UNKNOWN}
     */
    public String wireValue() {
        if (this == UNKNOWN) {
            throw new IllegalStateException(ERR_UNKNOWN);
        }
        return name();
    }

    /** Map a wire value to the enum, falling back to {@link #UNKNOWN}. */
    public static ConfirmationType fromWire(@Nullable String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
