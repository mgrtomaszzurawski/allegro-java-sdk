/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressRaw;
import org.jspecify.annotations.Nullable;

/**
 * The address an order's invoice is issued to. Exactly one of {@code company}
 * and {@code naturalPerson} identifies the invoice recipient.
 *
 * @param street street and building/flat
 * @param city city
 * @param zipCode postal code
 * @param countryCode ISO country code
 * @param company the company the invoice is for, or {@code null} for a private person
 * @param naturalPerson the person the invoice is for, or {@code null} for a company
 *
 * @since 0.7.0
 */
public record InvoiceAddress(
        String street,
        String city,
        String zipCode,
        String countryCode,
        @Nullable InvoiceCompany company,
        @Nullable InvoicePerson naturalPerson) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static InvoiceAddress from(CheckoutFormInvoiceAddressRaw raw) {
        var company = raw.getCompany();
        var naturalPerson = raw.getNaturalPerson();
        return new InvoiceAddress(
                raw.getStreet(),
                raw.getCity(),
                raw.getZipCode(),
                raw.getCountryCode(),
                company == null ? null : InvoiceCompany.from(company),
                naturalPerson == null ? null : InvoicePerson.from(naturalPerson));
    }
}
