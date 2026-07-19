/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * The paper size a printable (PDF) shipping label is laid out for.
 *
 * <p>Fail-soft on read (an unmodelled server value maps to {@link #UNKNOWN}) and
 * strict on write ({@link #UNKNOWN} cannot be serialized).
 *
 * @since 0.4.0
 */
public enum LabelPageSize {

    /** A4 sheet (spec value {@code A4}). */
    A4,

    /** A6 label sheet (spec value {@code A6}). */
    A6,

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
    public static LabelPageSize fromWire(@Nullable String raw) {
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
