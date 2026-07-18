/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.InvoiceDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderInvoice;
import java.util.List;

/**
 * Customer invoices on an order — reached via {@code orders().invoices()}.
 * Declaring an invoice is a two-step flow: {@link #declare} registers the
 * metadata and returns the new invoice id, then {@link #uploadFile} attaches the
 * file bytes to that id.
 *
 * @since 0.6.0
 */
public interface OrderInvoices {

    /**
     * List the invoices registered against an order.
     *
     * @param orderId the order identifier
     * @return the order's invoices; never {@code null}, possibly empty
     */
    List<OrderInvoice> ofOrder(String orderId);

    /**
     * Declare a new invoice on an order (metadata only; upload the file next).
     *
     * @param orderId the order identifier
     * @param declaration the invoice metadata
     * @return the id Allegro assigned the new invoice
     */
    String declare(String orderId, InvoiceDeclaration declaration);

    /**
     * Upload the file bytes for a previously declared invoice.
     *
     * @param orderId the order identifier
     * @param invoiceId the invoice id returned by {@link #declare}
     * @param file the invoice file bytes; sent as {@code application/pdf}
     */
    void uploadFile(String orderId, String invoiceId, byte[] file);
}
