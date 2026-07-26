/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * How a delivery option hands the parcel to the buyer. Read-only: this value
 * only ever arrives from the server on a delivery proposal, so an unmodelled
 * value maps to {@link #UNKNOWN} and there is no write path.
 *
 * @since 0.5.0
 */
public enum DeliveryType {

    /** Delivered to the buyer's door (spec value {@code DOOR}). */
    DOOR,

    /** Delivered to an automated parcel machine (spec value {@code APM}). */
    APM,

    /** Delivered to a pick-up / drop-off point (spec value {@code PUDO}). */
    PUDO,

    /** A value returned by the server that this SDK release does not model. */
    UNKNOWN;

    /** Map a wire value to the enum, falling back to {@link #UNKNOWN}. */
    public static DeliveryType fromWire(@Nullable String raw) {
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
