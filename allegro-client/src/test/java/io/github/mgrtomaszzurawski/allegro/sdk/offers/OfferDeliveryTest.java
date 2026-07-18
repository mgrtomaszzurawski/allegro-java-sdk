/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OfferDeliveryTest {

    private static final String SHIPPING_RATES_ID = "a1b2c3d4-0000-0000-0000-000000000001";
    private static final String HANDLING_TIME = "PT24H";
    private static final OffsetDateTime SHIPMENT_DATE =
            OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String ADDITIONAL_INFO = "Ships from Warsaw";

    @Test
    void build_whenAllFieldsSet_exposesEachValue() {
        // when
        OfferDelivery delivery = OfferDelivery.builder()
                .shippingRatesId(SHIPPING_RATES_ID)
                .handlingTime(HANDLING_TIME)
                .shipmentDate(SHIPMENT_DATE)
                .additionalInfo(ADDITIONAL_INFO)
                .build();

        // then
        assertEquals(SHIPPING_RATES_ID, delivery.shippingRatesId());
        assertEquals(HANDLING_TIME, delivery.handlingTime());
        assertEquals(SHIPMENT_DATE, delivery.shipmentDate());
        assertEquals(ADDITIONAL_INFO, delivery.additionalInfo());
    }

    @Test
    void build_whenNoFieldsSet_leavesEveryFieldNull() {
        // when — a partial delivery block is valid
        OfferDelivery delivery = OfferDelivery.builder().build();

        // then
        assertNull(delivery.shippingRatesId());
        assertNull(delivery.handlingTime());
        assertNull(delivery.shipmentDate());
        assertNull(delivery.additionalInfo());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        // given
        OfferDelivery original = OfferDelivery.builder()
                .shippingRatesId(SHIPPING_RATES_ID)
                .handlingTime(HANDLING_TIME)
                .shipmentDate(SHIPMENT_DATE)
                .additionalInfo(ADDITIONAL_INFO)
                .build();

        // when
        OfferDelivery copy = original.toBuilder().build();

        // then
        assertEquals(original.shippingRatesId(), copy.shippingRatesId());
        assertEquals(original.handlingTime(), copy.handlingTime());
        assertEquals(original.shipmentDate(), copy.shipmentDate());
        assertEquals(original.additionalInfo(), copy.additionalInfo());
    }

    @Test
    void from_whenResponsePresent_mapsEveryField() {
        // given — a generated delivery response block
        DeliveryProductOfferResponseRaw raw = new DeliveryProductOfferResponseRaw()
                .shippingRates(new JustIdRaw().id(SHIPPING_RATES_ID))
                .handlingTime(HANDLING_TIME)
                .shipmentDate(SHIPMENT_DATE)
                .additionalInfo(ADDITIONAL_INFO);

        // when
        OfferDelivery delivery = OfferDelivery.from(raw);

        // then
        assertEquals(SHIPPING_RATES_ID, delivery.shippingRatesId());
        assertEquals(HANDLING_TIME, delivery.handlingTime());
        assertEquals(SHIPMENT_DATE, delivery.shipmentDate());
        assertEquals(ADDITIONAL_INFO, delivery.additionalInfo());
    }

    @Test
    void from_whenNull_returnsNull() {
        // then — a null block maps to a null value, never an empty one
        assertNull(OfferDelivery.from(null));
    }

    @Test
    void from_whenShippingRatesAbsent_leavesIdNull() {
        // given — a block with no shipping-rate reference
        DeliveryProductOfferResponseRaw raw =
                new DeliveryProductOfferResponseRaw().handlingTime(HANDLING_TIME);

        // when
        OfferDelivery delivery = OfferDelivery.from(raw);

        // then
        assertNull(delivery.shippingRatesId());
        assertEquals(HANDLING_TIME, delivery.handlingTime());
    }
}
