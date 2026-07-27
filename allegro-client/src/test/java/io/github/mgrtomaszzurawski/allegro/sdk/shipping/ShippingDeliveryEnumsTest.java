/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryPayment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the read-only delivery-option enums: a known wire value maps to
 * its constant, and an unmodelled or absent value degrades to {@code UNKNOWN}
 * (fail-soft on read — these values only ever arrive from the server).
 */
class ShippingDeliveryEnumsTest {

    private static final String UNMODELLED = "SOME_FUTURE_VALUE";

    @Test
    void deliveryType_fromWire_mapsKnownAndDegradesUnknown() {
        // given/when/then
        assertEquals(DeliveryType.DOOR, DeliveryType.fromWire("DOOR"));
        assertEquals(DeliveryType.APM, DeliveryType.fromWire("APM"));
        assertEquals(DeliveryType.PUDO, DeliveryType.fromWire("PUDO"));
        assertEquals(DeliveryType.UNKNOWN, DeliveryType.fromWire(UNMODELLED));
        assertEquals(DeliveryType.UNKNOWN, DeliveryType.fromWire(null));
    }

    @Test
    void deliveryPayment_fromWire_mapsKnownAndDegradesUnknown() {
        // given/when/then
        assertEquals(DeliveryPayment.PREPAID, DeliveryPayment.fromWire("PREPAID"));
        assertEquals(DeliveryPayment.POSTPAID, DeliveryPayment.fromWire("POSTPAID"));
        assertEquals(DeliveryPayment.UNKNOWN, DeliveryPayment.fromWire(UNMODELLED));
        assertEquals(DeliveryPayment.UNKNOWN, DeliveryPayment.fromWire(null));
    }
}
