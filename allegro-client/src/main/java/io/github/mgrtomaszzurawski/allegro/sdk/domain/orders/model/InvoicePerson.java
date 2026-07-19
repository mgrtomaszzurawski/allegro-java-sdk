/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressNaturalPersonRaw;

/**
 * The private individual an order's invoice is addressed to (when the invoice is
 * issued to a natural person rather than a company).
 *
 * @param firstName the person's first name
 * @param lastName the person's last name
 *
 * @since 0.7.0
 */
public record InvoicePerson(String firstName, String lastName) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static InvoicePerson from(CheckoutFormInvoiceAddressNaturalPersonRaw raw) {
        return new InvoicePerson(raw.getFirstName(), raw.getLastName());
    }

    /**
     * Redacts the person's name so an accidental log or trace of an
     * {@code InvoicePerson} never leaks it; use the typed accessors to read the
     * fields deliberately.
     */
    @Override
    public String toString() {
        return "InvoicePerson[personal data redacted]";
    }
}
