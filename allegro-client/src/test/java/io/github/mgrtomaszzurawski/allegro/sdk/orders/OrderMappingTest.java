/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormFulfillmentStatusRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormStatusRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive mapping tests for the order status enums — every Layer-1 value
 * must resolve to the matching public value, since the seller acts on the
 * status the SDK reports.
 */
class OrderMappingTest {

    @Test
    void from_whenEveryOrderStatusRawValue_mapsToMatchingStatus() {
        // then
        assertEquals(OrderStatus.BOUGHT, OrderStatus.from(CheckoutFormStatusRaw.BOUGHT));
        assertEquals(OrderStatus.FILLED_IN, OrderStatus.from(CheckoutFormStatusRaw.FILLED_IN));
        assertEquals(OrderStatus.READY_FOR_PROCESSING,
                OrderStatus.from(CheckoutFormStatusRaw.READY_FOR_PROCESSING));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.from(CheckoutFormStatusRaw.CANCELLED));
    }

    @Test
    void from_whenEveryFulfillmentStatusRawValue_mapsToMatchingStatus() {
        // then
        assertEquals(SellerStatus.NEW, SellerStatus.from(CheckoutFormFulfillmentStatusRaw.NEW));
        assertEquals(SellerStatus.PROCESSING,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.PROCESSING));
        assertEquals(SellerStatus.READY_FOR_SHIPMENT,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.READY_FOR_SHIPMENT));
        assertEquals(SellerStatus.READY_FOR_PICKUP,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.READY_FOR_PICKUP));
        assertEquals(SellerStatus.SENT, SellerStatus.from(CheckoutFormFulfillmentStatusRaw.SENT));
        assertEquals(SellerStatus.PICKED_UP,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.PICKED_UP));
        assertEquals(SellerStatus.CANCELLED,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.CANCELLED));
        assertEquals(SellerStatus.SUSPENDED,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.SUSPENDED));
        assertEquals(SellerStatus.RETURNED,
                SellerStatus.from(CheckoutFormFulfillmentStatusRaw.RETURNED));
    }

    @Test
    void from_whenFulfillmentStatusRawNull_returnsNull() {
        // then — an order with no seller status yet maps to null, not a default
        assertNull(SellerStatus.from(null));
    }
}
