/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormPaymentProviderRaw;

/**
 * Which provider processed an order's payment. Each constant name mirrors the
 * Allegro spec value exactly (spec identifiers, the documented exception to the
 * short-name rule), so the short-variable PMD rule — which has no per-value
 * carve-out — is suppressed on the abbreviated constants only.
 *
 * @since 0.7.0
 */
public enum PaymentProvider {

    /** PayU. */
    PAYU,

    /** Przelewy24. */
    @SuppressWarnings("PMD.ShortVariableWithDomainExceptions")
    P24,

    /** Allegro Finance. */
    @SuppressWarnings("PMD.ShortVariableWithDomainExceptions")
    AF,

    /** Settled offline (no online provider). */
    OFFLINE,

    /** Electronic payment terminal. */
    @SuppressWarnings("PMD.ShortVariableWithDomainExceptions")
    EPT,

    /** A provider this SDK release does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public payment provider. */
    public static PaymentProvider from(CheckoutFormPaymentProviderRaw raw) {
        return switch (raw) {
            case PAYU -> PAYU;
            case P24 -> P24;
            case AF -> AF;
            case OFFLINE -> OFFLINE;
            case EPT -> EPT;
            default -> UNKNOWN;
        };
    }
}
