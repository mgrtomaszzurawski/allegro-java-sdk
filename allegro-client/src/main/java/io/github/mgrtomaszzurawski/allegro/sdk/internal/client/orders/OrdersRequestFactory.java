/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormAddWaybillRequestLineItemsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormAddWaybillRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormFulfillmentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemSetSerialNumbersEntriesRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemSetSerialNumbersEntryRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemSetSerialNumbersRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemsSetSerialNumbersRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NewOrderBillingDocumentLinkRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.SerialNumbersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import java.util.List;
import java.util.UUID;

/**
 * Builds the generated Layer-1 request bodies for the order-management write
 * endpoints from the public domain builders, so {@link OrdersImpl} stays a thin
 * verb dispatcher and the {@code *Raw} construction lives in one place.
 *
 * @since 0.4.0
 */
final class OrdersRequestFactory {

    private OrdersRequestFactory() {
    }

    /** Request body for {@code PUT /order/checkout-forms/{id}/fulfillment}. */
    static CheckoutFormFulfillmentRaw fulfillment(SellerStatus status) {
        return new CheckoutFormFulfillmentRaw().status(status.toRaw());
    }

    /** Request body for {@code POST /order/checkout-forms/{id}/serial-numbers}. */
    static CheckoutFormLineItemsSetSerialNumbersRequestRaw serialNumbers(SerialNumbersRequest request) {
        List<CheckoutFormLineItemSetSerialNumbersRequestRaw> lineItems = request.lineItems().stream()
                .map(OrdersRequestFactory::serialNumbersLineItem)
                .toList();
        return new CheckoutFormLineItemsSetSerialNumbersRequestRaw().lineItems(lineItems);
    }

    private static CheckoutFormLineItemSetSerialNumbersRequestRaw serialNumbersLineItem(
            SerialNumbersRequest.LineItemSerialNumbers entry) {
        List<CheckoutFormLineItemSetSerialNumbersEntryRequestRaw> entries = entry.serialNumbers().stream()
                .map(serial -> new CheckoutFormLineItemSetSerialNumbersEntryRequestRaw().value(serial))
                .toList();
        return new CheckoutFormLineItemSetSerialNumbersRequestRaw()
                .id(UUID.fromString(entry.lineItemId()))
                .serialNumbers(new CheckoutFormLineItemSetSerialNumbersEntriesRequestRaw().entries(entries));
    }

    /** Request body for {@code POST /order/{orderId}/billing-documents/links}. */
    static NewOrderBillingDocumentLinkRaw billingDocumentLink(String url) {
        return new NewOrderBillingDocumentLinkRaw().url(url);
    }

    /** Request body for {@code POST /order/checkout-forms/{id}/shipments}. */
    static CheckoutFormAddWaybillRequestRaw shipment(ShipmentRequest request) {
        List<String> lineItemIds = request.lineItemIds();
        return new CheckoutFormAddWaybillRequestRaw()
                .carrierId(request.carrierId())
                .waybill(request.waybill())
                .carrierName(request.carrierName())
                // Omit line items entirely when none given: an empty array would
                // mean "cover no items", a null means "cover the whole order".
                .lineItems(lineItemIds.isEmpty() ? null : lineItemIds.stream()
                        .map(id -> new CheckoutFormAddWaybillRequestLineItemsInnerRaw().id(id))
                        .toList());
    }
}
