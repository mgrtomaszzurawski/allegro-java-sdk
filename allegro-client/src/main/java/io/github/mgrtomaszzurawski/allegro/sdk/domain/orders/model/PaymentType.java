/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormPaymentTypeRaw;

/**
 * How an order was paid for. Each constant name mirrors the Allegro spec value
 * exactly.
 *
 * @since 0.7.0
 */
public enum PaymentType {

    /** Paid to the carrier on delivery (cash on delivery). */
    CASH_ON_DELIVERY,

    /** Paid by a bank wire transfer. */
    WIRE_TRANSFER,

    /** Paid online through a payment provider. */
    ONLINE,

    /** Paid via a split-payment arrangement. */
    SPLIT_PAYMENT,

    /** Paid on extended (deferred) payment terms. */
    EXTENDED_TERM,

    /** A payment type this SDK release does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public payment type. */
    public static PaymentType from(CheckoutFormPaymentTypeRaw raw) {
        return switch (raw) {
            case CASH_ON_DELIVERY -> CASH_ON_DELIVERY;
            case WIRE_TRANSFER -> WIRE_TRANSFER;
            case ONLINE -> ONLINE;
            case SPLIT_PAYMENT -> SPLIT_PAYMENT;
            case EXTENDED_TERM -> EXTENDED_TERM;
            default -> UNKNOWN;
        };
    }
}
