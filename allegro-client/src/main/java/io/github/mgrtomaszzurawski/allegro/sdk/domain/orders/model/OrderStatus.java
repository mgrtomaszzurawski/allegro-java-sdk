/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormStatusRaw;

/**
 * Buyer-side lifecycle status of an order (checkout form) — where the order is
 * in the purchase-and-payment flow, independent of the seller's own
 * {@link SellerStatus} handling status.
 *
 * @since 0.3.0
 */
public enum OrderStatus {

    /** The buyer completed the purchase; the order is ready to be processed. */
    BOUGHT,

    /** The buyer filled in the checkout form but has not finished payment. */
    FILLED_IN,

    /** Payment cleared and the order is ready for the seller to process. */
    READY_FOR_PROCESSING,

    /** The order was cancelled. */
    CANCELLED,

    /** A status this SDK release does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public status. */
    public static OrderStatus from(CheckoutFormStatusRaw raw) {
        return switch (raw) {
            case BOUGHT -> BOUGHT;
            case FILLED_IN -> FILLED_IN;
            case READY_FOR_PROCESSING -> READY_FOR_PROCESSING;
            case CANCELLED -> CANCELLED;
            default -> UNKNOWN;
        };
    }
}
