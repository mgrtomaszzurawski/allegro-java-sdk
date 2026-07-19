/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * The file format a carrier produces the shipping label in.
 *
 * <p>Fail-soft on read (an unmodelled server value maps to {@link #UNKNOWN}) and
 * strict on write ({@link #UNKNOWN} cannot be serialized).
 *
 * @since 0.4.0
 */
public enum LabelFormat {

    /** Portable Document Format label (spec value {@code PDF}). */
    PDF,

    /** Zebra Programming Language thermal-printer label (spec value {@code ZPL}). */
    ZPL,

    /** Eltron Programming Language thermal-printer label (spec value {@code EPL}). */
    EPL,

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
    public static LabelFormat fromWire(@Nullable String raw) {
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
