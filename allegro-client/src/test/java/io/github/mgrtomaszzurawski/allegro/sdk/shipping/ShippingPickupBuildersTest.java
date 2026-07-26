/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupProposalsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Builder round-trip and fail-fast tests for the pickup request models: each
 * required field is proven to fail the build when missing, and {@code toBuilder}
 * is proven to preserve the assembled state.
 */
class ShippingPickupBuildersTest {

    private static final String SHIPMENT_ID = "SHIP-1001";
    private static final String READY_DATE = "2026-07-28";
    private static final String PICKUP_DATE = "2026-07-28";
    private static final String MIN_TIME = "08:00";
    private static final String MAX_TIME = "16:00";

    private static PostalAddress address() {
        return PostalAddress.builder()
                .street("Grunwaldzka 100").postalCode("80-244").city("Gdansk")
                .email("seller@example.com").phone("+48500100100").build();
    }

    private static PickupTime window() {
        return PickupTime.of(PICKUP_DATE, MIN_TIME, MAX_TIME);
    }

    @Test
    void pickupTime_of_setsFields() {
        // given/when
        PickupTime time = PickupTime.of(PICKUP_DATE, MIN_TIME, MAX_TIME);

        // then
        assertEquals(PICKUP_DATE, time.date());
        assertEquals(MIN_TIME, time.minTime());
        assertEquals(MAX_TIME, time.maxTime());
    }

    @Test
    void pickupRequest_whenAllRequiredSet_builds() {
        // given/when
        PickupRequest request = PickupRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID))
                .pickupTime(window())
                .address(address())
                .build();

        // then
        assertEquals(List.of(SHIPMENT_ID), request.shipmentIds());
        assertEquals(PICKUP_DATE, request.pickupTime().date());
    }

    @Test
    void pickupRequest_whenShipmentIdsMissing_throws() {
        // given
        var builder = PickupRequest.builder().pickupTime(window()).address(address());

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void pickupRequest_whenPickupTimeMissing_throws() {
        // given
        var builder = PickupRequest.builder().shipmentIds(List.of(SHIPMENT_ID)).address(address());

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void pickupRequest_whenAddressMissing_throws() {
        // given
        var builder = PickupRequest.builder().shipmentIds(List.of(SHIPMENT_ID)).pickupTime(window());

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void pickupRequest_toBuilder_preservesState() {
        // given
        PickupRequest original = PickupRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID)).pickupTime(window()).address(address()).build();

        // when
        PickupRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.shipmentIds(), copy.shipmentIds());
        assertEquals(original.pickupTime().date(), copy.pickupTime().date());
        assertEquals(original.address().city(), copy.address().city());
    }

    @Test
    void pickupProposalsRequest_whenRequiredSet_builds() {
        // given/when
        PickupProposalsRequest request = PickupProposalsRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID))
                .readyDate(READY_DATE)
                .address(address())
                .build();

        // then
        assertEquals(List.of(SHIPMENT_ID), request.shipmentIds());
        assertEquals(READY_DATE, request.readyDate());
    }

    @Test
    void pickupProposalsRequest_whenShipmentIdsMissing_throws() {
        // given
        var builder = PickupProposalsRequest.builder().address(address());

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void pickupProposalsRequest_whenAddressMissing_throws() {
        // given
        var builder = PickupProposalsRequest.builder().shipmentIds(List.of(SHIPMENT_ID));

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void pickupProposalsRequest_toBuilder_preservesState() {
        // given
        PickupProposalsRequest original = PickupProposalsRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID)).readyDate(READY_DATE).address(address()).build();

        // when
        PickupProposalsRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.shipmentIds(), copy.shipmentIds());
        assertEquals(original.readyDate(), copy.readyDate());
    }
}
