/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckFormsNewOrderInvoiceFileRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckFormsNewOrderInvoiceIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckFormsNewOrderInvoiceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormsOrderInvoiceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormsOrderInvoicesRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.OrderInvoices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.InvoiceDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderInvoice;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;

/**
 * Endpoint wrappers behind the {@link OrderInvoices} sub-facade.
 *
 * @since 0.6.0
 */
public final class OrderInvoicesImpl implements OrderInvoices {

    private static final String OP_LIST = "list order invoices";
    private static final String OP_DECLARE = "declare order invoice";
    private static final String OP_UPLOAD = "upload order invoice file";
    private static final String INVOICE_CONTENT_TYPE = "application/pdf";

    private final HttpSupport http;

    public OrderInvoicesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<OrderInvoice> ofOrder(String orderId) {
        CheckoutFormsOrderInvoicesRaw response = http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId, ApiPaths.INVOICES_SEGMENT),
                CheckoutFormsOrderInvoicesRaw.class, OP_LIST);
        List<CheckoutFormsOrderInvoiceRaw> invoices = response.getInvoices();
        return invoices == null ? List.of() : invoices.stream().map(OrderInvoice::from).toList();
    }

    @Override
    public String declare(String orderId, InvoiceDeclaration declaration) {
        CheckFormsNewOrderInvoiceRaw body = new CheckFormsNewOrderInvoiceRaw()
                .invoiceNumber(declaration.invoiceNumber())
                ._file(new CheckFormsNewOrderInvoiceFileRaw().name(declaration.fileName()));
        CheckFormsNewOrderInvoiceIdRaw created = http.postJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId, ApiPaths.INVOICES_SEGMENT),
                body, CheckFormsNewOrderInvoiceIdRaw.class, OP_DECLARE);
        return created.getId();
    }

    @Override
    public void uploadFile(String orderId, String invoiceId, byte[] file) {
        http.request(OP_UPLOAD)
                .put(ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId,
                        ApiPaths.INVOICES_SEGMENT, invoiceId, ApiPaths.FILE_SEGMENT))
                .binaryBody(file, INVOICE_CONTENT_TYPE)
                .send();
    }
}
