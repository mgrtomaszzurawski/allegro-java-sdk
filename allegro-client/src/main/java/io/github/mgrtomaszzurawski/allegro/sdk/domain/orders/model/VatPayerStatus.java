/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressCompanyRaw.VatPayerStatusEnum;

/**
 * VAT-payer status of a company on an order's invoice address. Each constant
 * name mirrors the Allegro spec value exactly.
 *
 * @since 0.7.0
 */
public enum VatPayerStatus {

    /** An active VAT payer. */
    ACTIVE,

    /** Not an active VAT payer. */
    NON_ACTIVE,

    /** VAT status does not apply to this company. */
    NOT_APPLICABLE,

    /** A status this SDK release does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public VAT-payer status. */
    public static VatPayerStatus from(VatPayerStatusEnum raw) {
        return switch (raw) {
            case ACTIVE -> ACTIVE;
            case NON_ACTIVE -> NON_ACTIVE;
            case NOT_APPLICABLE -> NOT_APPLICABLE;
            default -> UNKNOWN;
        };
    }
}
