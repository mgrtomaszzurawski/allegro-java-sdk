/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressCompanyIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressCompanyRaw;
import java.util.List;

/**
 * The company an order's invoice is addressed to.
 *
 * <p>The company's tax identifiers are exposed as the typed {@link #taxIds()} list
 * (the current spec representation); the spec's deprecated single {@code taxId}
 * string is not modelled — read the same value from {@code taxIds()}.
 *
 * @param name company name
 * @param vatPayerStatus the company's VAT-payer status
 * @param taxIds the company's tax identifiers; never {@code null}, possibly empty
 *
 * @since 0.7.0
 */
public record InvoiceCompany(
        String name,
        VatPayerStatus vatPayerStatus,
        List<CompanyTaxId> taxIds) {

    public InvoiceCompany {
        taxIds = taxIds == null ? List.of() : List.copyOf(taxIds);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static InvoiceCompany from(CheckoutFormInvoiceAddressCompanyRaw raw) {
        List<CheckoutFormInvoiceAddressCompanyIdRaw> rawIds = raw.getIds();
        return new InvoiceCompany(
                raw.getName(),
                VatPayerStatus.from(raw.getVatPayerStatus()),
                rawIds == null ? List.of() : rawIds.stream().map(CompanyTaxId::from).toList());
    }
}
