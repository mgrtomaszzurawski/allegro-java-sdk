/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventTypeRaw;

/**
 * Kind of change reported by an entry in the seller's order event log
 * (streamed via {@code orders().streamEvents(...)}). Each constant name mirrors
 * the Allegro spec value exactly, so it doubles as the {@code type} filter value
 * on the wire.
 *
 * @since 0.4.0
 */
public enum OrderEventType {

    /** A buyer committed to the purchase (order created). */
    BOUGHT,

    /** The buyer completed the checkout form details. */
    FILLED_IN,

    /** The order is ready for the seller to process. */
    READY_FOR_PROCESSING,

    /** The buyer cancelled the order. */
    BUYER_CANCELLED,

    /** The seller-side fulfillment status changed. */
    FULFILLMENT_STATUS_CHANGED,

    /** The buyer modified the order after purchase. */
    BUYER_MODIFIED,

    /** The platform auto-cancelled the order (e.g. payment never arrived). */
    AUTO_CANCELLED,

    /**
     * An event type this SDK release does not model yet (read-only forward-compat
     * sentinel). Do NOT pass it as a {@code streamEvents} type filter — it is not a
     * real wire value; the filter path should drop it (guard pending, BACKLOG C3).
     */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public event type. */
    public static OrderEventType from(OrderEventTypeRaw raw) {
        return switch (raw) {
            case BOUGHT -> BOUGHT;
            case FILLED_IN -> FILLED_IN;
            case READY_FOR_PROCESSING -> READY_FOR_PROCESSING;
            case BUYER_CANCELLED -> BUYER_CANCELLED;
            case FULFILLMENT_STATUS_CHANGED -> FULFILLMENT_STATUS_CHANGED;
            case BUYER_MODIFIED -> BUYER_MODIFIED;
            case AUTO_CANCELLED -> AUTO_CANCELLED;
            default -> UNKNOWN;
        };
    }
}
