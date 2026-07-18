/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.PointsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.SerialNumbersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEventType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Round-trip and validation tests for the bucket B order builders. */
class OrderBuildersTest {

    private static final OffsetDateTime BOUGHT_FROM =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime BOUGHT_TO =
            OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime UPDATED_FROM =
            OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime UPDATED_TO =
            OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String PROVIDER_ID = "ALLEGRO";
    private static final String BUYER_LOGIN = "test-buyer";
    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String PAYMENT_ID = "pay-1";
    private static final String SURCHARGE_ID = "sur-1";
    private static final String DELIVERY_METHOD_ID = "dm-1";
    private static final String CARRIER_ID = "DPD";
    private static final String CARRIER_NAME = "DPD Express";
    private static final String WAYBILL = "00123456789";
    private static final String LINE_ITEM_ID = "0f3e2b1a-1111-2222-3333-444455556666";
    private static final String SERIAL_ONE = "SN-001";
    private static final String SERIAL_TWO = "SN-002";

    @Test
    void orderFilter_whenAll_hasNoCriteria() {
        // when
        OrderFilter filter = OrderFilter.all();

        // then
        assertTrue(filter.statuses().isEmpty());
        assertTrue(filter.fulfillmentStatuses().isEmpty());
        assertNull(filter.fulfillmentProviderId());
        assertNull(filter.lineItemsSent());
        assertNull(filter.boughtFrom());
        assertNull(filter.buyerLogin());
    }

    @Test
    void orderFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when — varargs overloads for the repeated fields, scalar setters for the rest
        OrderFilter filter = OrderFilter.builder()
                .statuses(OrderStatus.READY_FOR_PROCESSING)
                .fulfillmentStatuses(SellerStatus.NEW, SellerStatus.PROCESSING)
                .fulfillmentProviderId(PROVIDER_ID)
                .lineItemsSent(Boolean.FALSE)
                .boughtFrom(BOUGHT_FROM)
                .boughtTo(BOUGHT_TO)
                .updatedFrom(UPDATED_FROM)
                .updatedTo(UPDATED_TO)
                .buyerLogin(BUYER_LOGIN)
                .marketplaceId(MARKETPLACE_ID)
                .paymentId(PAYMENT_ID)
                .surchargeId(SURCHARGE_ID)
                .deliveryMethodId(DELIVERY_METHOD_ID)
                .build();

        // then
        assertEquals(List.of(OrderStatus.READY_FOR_PROCESSING), filter.statuses());
        assertEquals(List.of(SellerStatus.NEW, SellerStatus.PROCESSING), filter.fulfillmentStatuses());
        assertEquals(PROVIDER_ID, filter.fulfillmentProviderId());
        assertEquals(Boolean.FALSE, filter.lineItemsSent());
        assertEquals(BOUGHT_FROM, filter.boughtFrom());
        assertEquals(BOUGHT_TO, filter.boughtTo());
        assertEquals(UPDATED_FROM, filter.updatedFrom());
        assertEquals(UPDATED_TO, filter.updatedTo());
        assertEquals(BUYER_LOGIN, filter.buyerLogin());
        assertEquals(MARKETPLACE_ID, filter.marketplaceId());
        assertEquals(PAYMENT_ID, filter.paymentId());
        assertEquals(SURCHARGE_ID, filter.surchargeId());
        assertEquals(DELIVERY_METHOD_ID, filter.deliveryMethodId());

        // and toBuilder (List overloads) preserves everything
        OrderFilter copy = filter.toBuilder().build();
        assertEquals(filter.statuses(), copy.statuses());
        assertEquals(filter.fulfillmentStatuses(), copy.fulfillmentStatuses());
        assertEquals(filter.updatedTo(), copy.updatedTo());
        assertEquals(filter.deliveryMethodId(), copy.deliveryMethodId());
    }

    @Test
    void orderEventFilter_whenAll_isEmpty() {
        // then
        assertTrue(OrderEventFilter.all().types().isEmpty());
    }

    @Test
    void orderEventFilter_whenTypesSet_buildsAndToBuilderPreserves() {
        // when
        OrderEventFilter filter = OrderEventFilter.ofTypes(
                OrderEventType.BOUGHT, OrderEventType.FULFILLMENT_STATUS_CHANGED);

        // then
        assertEquals(List.of(OrderEventType.BOUGHT, OrderEventType.FULFILLMENT_STATUS_CHANGED),
                filter.types());
        assertEquals(filter.types(), filter.toBuilder().build().types());
    }

    @Test
    void pointsFilter_whenAll_isEmpty() {
        // then
        assertTrue(PointsFilter.all().carrierCodes().isEmpty());
    }

    @Test
    void pointsFilter_whenCarriersSet_buildsAndToBuilderPreserves() {
        // when
        PointsFilter filter = PointsFilter.ofCarriers("UPS", "DPD");

        // then
        assertEquals(List.of("UPS", "DPD"), filter.carrierCodes());
        assertEquals(filter.carrierCodes(), filter.toBuilder().build().carrierCodes());
    }

    @Test
    void shipmentRequest_whenRequiredFieldsOnly_builds() {
        // when
        ShipmentRequest request = ShipmentRequest.builder()
                .carrierId(CARRIER_ID)
                .waybill(WAYBILL)
                .build();

        // then
        assertEquals(CARRIER_ID, request.carrierId());
        assertEquals(WAYBILL, request.waybill());
        assertNull(request.carrierName());
        assertTrue(request.lineItemIds().isEmpty());
    }

    @Test
    void shipmentRequest_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when — varargs lineItemIds overload
        ShipmentRequest request = ShipmentRequest.builder()
                .carrierId(CARRIER_ID)
                .waybill(WAYBILL)
                .carrierName(CARRIER_NAME)
                .lineItemIds(LINE_ITEM_ID)
                .build();

        // then
        assertEquals(CARRIER_NAME, request.carrierName());
        assertEquals(List.of(LINE_ITEM_ID), request.lineItemIds());

        // and toBuilder (List overload) preserves everything
        ShipmentRequest copy = request.toBuilder().build();
        assertEquals(CARRIER_ID, copy.carrierId());
        assertEquals(WAYBILL, copy.waybill());
        assertEquals(CARRIER_NAME, copy.carrierName());
        assertEquals(List.of(LINE_ITEM_ID), copy.lineItemIds());
    }

    @Test
    void shipmentRequest_whenCarrierIdMissing_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> ShipmentRequest.builder().waybill(WAYBILL).build());
    }

    @Test
    void shipmentRequest_whenWaybillBlank_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> ShipmentRequest.builder().carrierId(CARRIER_ID).waybill(" ").build());
    }

    @Test
    void serialNumbersRequest_whenOneLineItem_builds() {
        // when — varargs serial numbers overload
        SerialNumbersRequest request = SerialNumbersRequest.builder()
                .lineItem(LINE_ITEM_ID, SERIAL_ONE, SERIAL_TWO)
                .build();

        // then
        assertEquals(1, request.lineItems().size());
        assertEquals(LINE_ITEM_ID, request.lineItems().get(0).lineItemId());
        assertEquals(List.of(SERIAL_ONE, SERIAL_TWO), request.lineItems().get(0).serialNumbers());
    }

    @Test
    void serialNumbersRequest_whenMultipleLineItems_buildsAndToBuilderPreserves() {
        // when
        SerialNumbersRequest request = SerialNumbersRequest.builder()
                .lineItem(LINE_ITEM_ID, List.of(SERIAL_ONE))
                .lineItem(LINE_ITEM_ID, List.of(SERIAL_TWO))
                .build();

        // then
        assertEquals(2, request.lineItems().size());

        // and toBuilder (List overload) preserves the entries
        SerialNumbersRequest copy = request.toBuilder().build();
        assertEquals(2, copy.lineItems().size());
        assertEquals(List.of(SERIAL_TWO), copy.lineItems().get(1).serialNumbers());
    }

    @Test
    void serialNumbersRequest_whenNoLineItems_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class, () -> SerialNumbersRequest.builder().build());
    }

    @Test
    void serialNumbersRequest_whenSerialNumbersEmpty_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> SerialNumbersRequest.builder().lineItem(LINE_ITEM_ID, List.of()).build());
    }

    @Test
    void serialNumbersRequest_whenLineItemIdBlank_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> SerialNumbersRequest.builder().lineItem(" ", SERIAL_ONE).build());
    }
}
