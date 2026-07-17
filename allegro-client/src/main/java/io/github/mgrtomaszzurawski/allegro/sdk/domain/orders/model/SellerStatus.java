/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormFulfillmentStatusRaw;
import org.jspecify.annotations.Nullable;

/**
 * Seller handling status of an order — the value the seller advances as the
 * order is prepared and shipped, distinct from the buyer-side
 * {@link OrderStatus}. Updating it lands with the order write endpoints in the
 * orders bucket body.
 *
 * @since 0.3.0
 */
public enum SellerStatus {

    /**
     * Not yet picked up by the seller for processing. The name mirrors the
     * Allegro spec value {@code NEW} exactly (a spec identifier, the documented
     * exception to the short-name rule), so the PMD short-variable rule — which
     * has no per-value carve-out — is suppressed on this constant only.
     */
    @SuppressWarnings("PMD.ShortVariableWithDomainExceptions")
    NEW,

    /** The seller has started preparing the order. */
    PROCESSING,

    /** Prepared and awaiting handover to the carrier. */
    READY_FOR_SHIPMENT,

    /** Prepared and awaiting buyer pickup. */
    READY_FOR_PICKUP,

    /** Handed to the carrier. */
    SENT,

    /** Collected by the buyer. */
    PICKED_UP,

    /** Handling was cancelled. */
    CANCELLED,

    /** Handling is temporarily suspended. */
    SUSPENDED,

    /** The order was returned. */
    RETURNED;

    /**
     * Map the generated Layer-1 enum to the public status, or {@code null} when
     * the order carries no seller status yet.
     */
    public static @Nullable SellerStatus from(@Nullable CheckoutFormFulfillmentStatusRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case NEW -> NEW;
            case PROCESSING -> PROCESSING;
            case READY_FOR_SHIPMENT -> READY_FOR_SHIPMENT;
            case READY_FOR_PICKUP -> READY_FOR_PICKUP;
            case SENT -> SENT;
            case PICKED_UP -> PICKED_UP;
            case CANCELLED -> CANCELLED;
            case SUSPENDED -> SUSPENDED;
            case RETURNED -> RETURNED;
        };
    }
}
