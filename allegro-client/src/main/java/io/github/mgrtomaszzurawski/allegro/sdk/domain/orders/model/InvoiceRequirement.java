/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceInfoRaw;
import org.jspecify.annotations.Nullable;

/**
 * Whether the buyer requested a VAT invoice for an order, and the invoicing
 * details when they did.
 *
 * <p>Invoice feature flags are not modelled; the requirement flag, due date, and
 * address are the seller-actionable values.
 *
 * @param required {@code true} when the buyer requested an invoice
 * @param dueDate the invoice due date, or {@code null} when not set
 * @param address the address to invoice, or {@code null} when the buyer requested
 *     no invoice or supplied no address
 *
 * @since 0.7.0
 */
public record InvoiceRequirement(
        boolean required,
        @Nullable String dueDate,
        @Nullable InvoiceAddress address) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static InvoiceRequirement from(CheckoutFormInvoiceInfoRaw raw) {
        var address = raw.getAddress();
        return new InvoiceRequirement(
                raw.getRequired(),
                raw.getDueDate(),
                address == null ? null : InvoiceAddress.from(address));
    }
}
