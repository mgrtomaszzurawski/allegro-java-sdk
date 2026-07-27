/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.AlreadyInWarehouseShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CourierBySellerShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CourierRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OwnTransportShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThirdPartyDeliveryShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThirdPartyRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnShipping;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The read mapping {@link AsnShipping#from} for every delivery method and its
 * null-degrade branches — the write side is wire-verified in
 * {@code AdvanceShipNoticesClientTest}, the four-method deserialization in
 * {@code AsnShippingLayer1RegressionTest}; this covers the domain projection itself.
 */
class AsnShippingMappingTest {

    private static final String COURIER_ID = "DPD";
    private static final String TRACK_NUMBER = "TRACK-1";
    private static final String TRUCK_PLATE = "FZ12453";
    private static final String CARRIER_NAME = "Company ABC";
    private static final String ORDER_NUMBER = "ORD-9";
    private static final String COUNTRY = "PL";
    private static final String FUTURE_METHOD = "SOME_FUTURE_METHOD";
    private static final OffsetDateTime ETA = OffsetDateTime.parse("2026-07-15T08:00:00Z");

    @Test
    void from_whenCourierBySeller_mapsIdAndTrackingNumbers() {
        CourierBySellerShippingRaw raw = new CourierBySellerShippingRaw()
                .courier(new CourierRaw().id(COURIER_ID).trackingNumbers(List.of(TRACK_NUMBER)))
                .estimatedTimeOfArrival(ETA).countryCode(COUNTRY);

        AsnShipping.CourierBySeller courier =
                assertInstanceOf(AsnShipping.CourierBySeller.class, AsnShipping.from(raw));
        assertEquals(COURIER_ID, courier.courierId());
        assertEquals(List.of(TRACK_NUMBER), courier.trackingNumbers());
        assertEquals(ETA, courier.estimatedTimeOfArrival());
        assertEquals(COUNTRY, courier.countryCode());
    }

    @Test
    void from_whenCourierObjectAbsent_degradesIdToNullAndTrackingToEmpty() {
        CourierBySellerShippingRaw raw = new CourierBySellerShippingRaw()
                .estimatedTimeOfArrival(ETA).countryCode(COUNTRY);

        AsnShipping.CourierBySeller courier =
                assertInstanceOf(AsnShipping.CourierBySeller.class, AsnShipping.from(raw));
        assertNull(courier.courierId());
        assertEquals(List.of(), courier.trackingNumbers());
    }

    @Test
    void from_whenOwnTransport_mapsTruckPlate() {
        OwnTransportShippingRaw raw = new OwnTransportShippingRaw()
                .truckLicencePlate(TRUCK_PLATE).estimatedTimeOfArrival(ETA).countryCode(COUNTRY);

        AsnShipping.OwnTransport own =
                assertInstanceOf(AsnShipping.OwnTransport.class, AsnShipping.from(raw));
        assertEquals(TRUCK_PLATE, own.truckLicencePlate());
        assertEquals(COUNTRY, own.countryCode());
    }

    @Test
    void from_whenThirdPartyDelivery_mapsNameAndOrderNumber() {
        ThirdPartyDeliveryShippingRaw raw = new ThirdPartyDeliveryShippingRaw()
                .thirdParty(new ThirdPartyRaw().name(CARRIER_NAME).orderNumber(ORDER_NUMBER))
                .estimatedTimeOfArrival(ETA).countryCode(COUNTRY);

        AsnShipping.ThirdPartyDelivery third =
                assertInstanceOf(AsnShipping.ThirdPartyDelivery.class, AsnShipping.from(raw));
        assertEquals(CARRIER_NAME, third.carrierName());
        assertEquals(ORDER_NUMBER, third.orderNumber());
    }

    @Test
    void from_whenThirdPartyObjectAbsent_degradesNameAndOrderToNull() {
        ThirdPartyDeliveryShippingRaw raw = new ThirdPartyDeliveryShippingRaw()
                .estimatedTimeOfArrival(ETA).countryCode(COUNTRY);

        AsnShipping.ThirdPartyDelivery third =
                assertInstanceOf(AsnShipping.ThirdPartyDelivery.class, AsnShipping.from(raw));
        assertNull(third.carrierName());
        assertNull(third.orderNumber());
    }

    @Test
    void from_whenAlreadyInWarehouse_mapsCommonFields() {
        AlreadyInWarehouseShippingRaw raw = new AlreadyInWarehouseShippingRaw()
                .estimatedTimeOfArrival(ETA).countryCode(COUNTRY);

        AsnShipping.AlreadyInWarehouse warehouse =
                assertInstanceOf(AsnShipping.AlreadyInWarehouse.class, AsnShipping.from(raw));
        assertEquals(ETA, warehouse.estimatedTimeOfArrival());
        assertEquals(COUNTRY, warehouse.countryCode());
    }

    @Test
    void from_whenNull_returnsNull() {
        assertNull(AsnShipping.from(null));
    }

    @Test
    void from_whenUnmodelledMethod_returnsNull() {
        // a base ShippingRaw (e.g. an unknown method rescued to the base) is not one of the
        // four modelled subtypes — forward-compatible degrade to null, not a throw
        assertNull(AsnShipping.from(new ShippingRaw().method(FUTURE_METHOD)));
    }
}
