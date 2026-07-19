/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * The kind of parcel a shipment package holds, which determines how the carrier
 * handles and prices it.
 *
 * <p>Fail-soft on read (an unmodelled server value maps to {@link #UNKNOWN}) and
 * strict on write ({@link #UNKNOWN} cannot be serialized).
 *
 * @since 0.4.0
 */
public enum PackageType {

    /** A document envelope (spec value {@code DOX}). */
    DOX,

    /** A standard parcel (spec value {@code PACKAGE}). */
    PACKAGE,

    /** A pallet (spec value {@code PALLET}). */
    PALLET,

    /** Any other package kind the carrier supports (spec value {@code OTHER}). */
    OTHER,

    /** A value returned by the server that this SDK release does not model. */
    UNKNOWN;

    private static final String ERR_UNKNOWN =
            "UNKNOWN is a read-only sentinel and cannot be sent to Allegro";

    /**
     * Wire representation to send to Allegro.
     *
     * @throws IllegalStateException if called on {@link #UNKNOWN}, which never
     *     originates from consumer input on a write path
     */
    public String wireValue() {
        if (this == UNKNOWN) {
            throw new IllegalStateException(ERR_UNKNOWN);
        }
        return name();
    }

    /** Map a wire value to the enum, falling back to {@link #UNKNOWN}. */
    public static PackageType fromWire(@Nullable String raw) {
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
