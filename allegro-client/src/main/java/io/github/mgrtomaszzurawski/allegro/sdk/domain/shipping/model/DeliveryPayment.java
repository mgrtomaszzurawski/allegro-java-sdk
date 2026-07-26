/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * When the carrier is paid for a delivery option. Read-only: this value only
 * ever arrives from the server on a delivery proposal, so an unmodelled value
 * maps to {@link #UNKNOWN} and there is no write path.
 *
 * @since 0.5.0
 */
public enum DeliveryPayment {

    /** Paid up front by the seller (spec value {@code PREPAID}). */
    PREPAID,

    /** Paid on delivery (spec value {@code POSTPAID}). */
    POSTPAID,

    /** A value returned by the server that this SDK release does not model. */
    UNKNOWN;

    /** Map a wire value to the enum, falling back to {@link #UNKNOWN}. */
    public static DeliveryPayment fromWire(@Nullable String raw) {
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
