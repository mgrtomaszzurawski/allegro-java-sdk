/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressCompanyRaw;
import org.jspecify.annotations.Nullable;

/**
 * The company an order's invoice is addressed to.
 *
 * @param name company name
 * @param vatPayerStatus the company's VAT-payer status
 * @param taxId the company's tax identifier (NIP), or {@code null} when not provided
 *
 * @since 0.7.0
 */
public record InvoiceCompany(
        String name,
        VatPayerStatus vatPayerStatus,
        @Nullable String taxId) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static InvoiceCompany from(CheckoutFormInvoiceAddressCompanyRaw raw) {
        return new InvoiceCompany(
                raw.getName(),
                VatPayerStatus.from(raw.getVatPayerStatus()),
                raw.getTaxId());
    }
}
