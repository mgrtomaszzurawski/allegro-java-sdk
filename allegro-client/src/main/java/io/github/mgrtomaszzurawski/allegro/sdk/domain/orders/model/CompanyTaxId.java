/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressCompanyIdRaw;

/**
 * One tax identifier of a company on an order's invoice address — a typed
 * {@code (type, value)} pair (e.g. {@code PL_NIP} → {@code "1234567890"}).
 *
 * @param type the kind of tax identifier
 * @param value the identifier value
 *
 * @since 0.7.0
 */
public record CompanyTaxId(CompanyTaxIdType type, String value) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static CompanyTaxId from(CheckoutFormInvoiceAddressCompanyIdRaw raw) {
        return new CompanyTaxId(CompanyTaxIdType.from(raw.getType()), raw.getValue());
    }
}
