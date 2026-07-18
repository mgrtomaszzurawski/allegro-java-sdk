/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormsOrderInvoiceFileRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormsOrderInvoiceRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * An invoice registered against an order (from
 * {@code orders().invoices().ofOrder(orderId)}).
 *
 * @param id invoice identifier (pass to {@code uploadFile} to attach the file),
 *     or {@code null} when absent
 * @param invoiceNumber the seller's invoice number, or {@code null} when absent
 * @param createdAt when the invoice was registered, or {@code null}
 * @param fileName the attached file name, or {@code null} when no file is attached
 *
 * @since 0.6.0
 */
public record OrderInvoice(
        @Nullable String id,
        @Nullable String invoiceNumber,
        @Nullable OffsetDateTime createdAt,
        @Nullable String fileName) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static OrderInvoice from(CheckoutFormsOrderInvoiceRaw raw) {
        CheckoutFormsOrderInvoiceFileRaw file = raw.getFile();
        return new OrderInvoice(
                raw.getId(),
                raw.getInvoiceNumber(),
                raw.getCreatedAt(),
                file == null ? null : file.getName());
    }
}
