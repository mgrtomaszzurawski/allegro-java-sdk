/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw.PaymentPolicyEnum;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PaymentPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import org.junit.jupiter.api.Test;

/**
 * The point-of-service enums are fail-soft on read (unmodelled server values map
 * to {@code UNKNOWN}) and strict on write ({@code UNKNOWN} cannot be serialized).
 * {@link PaymentPolicy} is a closed enum mapped by name from the typed Layer-1
 * enum, so a dedicated parity test locks that the two never diverge.
 */
class ShippingEnumsTest {

    private static final String UNMODELLED = "PARCEL_LOCKER";

    @Test
    void fromWire_whenKnownValue_mapsToEnum() {
        assertEquals(PosType.PICKUP_POINT, PosType.fromWire("PICKUP_POINT"));
        assertEquals(PosStatus.TEMPORARILY_CLOSED, PosStatus.fromWire("TEMPORARILY_CLOSED"));
        assertEquals(ConfirmationType.AWAIT_CONTACT, ConfirmationType.fromWire("AWAIT_CONTACT"));
    }

    @Test
    void fromWire_whenUnmodelledValue_mapsToUnknown() {
        assertEquals(PosType.UNKNOWN, PosType.fromWire(UNMODELLED));
        assertEquals(PosStatus.UNKNOWN, PosStatus.fromWire(UNMODELLED));
        assertEquals(ConfirmationType.UNKNOWN, ConfirmationType.fromWire(UNMODELLED));
    }

    @Test
    void fromWire_whenNull_mapsToUnknown() {
        assertEquals(PosType.UNKNOWN, PosType.fromWire(null));
        assertEquals(PosStatus.UNKNOWN, PosStatus.fromWire(null));
        assertEquals(ConfirmationType.UNKNOWN, ConfirmationType.fromWire(null));
    }

    @Test
    void wireValue_whenKnownValue_isTheWireString() {
        assertEquals("PICKUP_POINT", PosType.PICKUP_POINT.wireValue());
        assertEquals("ACTIVE", PosStatus.ACTIVE.wireValue());
        assertEquals("CONTACT_NOT_REQUIRED", ConfirmationType.CONTACT_NOT_REQUIRED.wireValue());
    }

    @Test
    void wireValue_whenUnknown_throws() {
        assertThrows(IllegalStateException.class, PosType.UNKNOWN::wireValue);
        assertThrows(IllegalStateException.class, PosStatus.UNKNOWN::wireValue);
        assertThrows(IllegalStateException.class, ConfirmationType.UNKNOWN::wireValue);
    }

    @Test
    void paymentPolicy_domainAndRawEnumsShareEveryName() {
        // DeliveryMethod maps PaymentPolicyEnum -> PaymentPolicy by name; this
        // fails the build if a future spec regeneration ever adds a value on one
        // side only (the closed enum has no UNKNOWN fallback to absorb it).
        for (PaymentPolicyEnum raw : PaymentPolicyEnum.values()) {
            assertDoesNotThrow(() -> PaymentPolicy.valueOf(raw.name()),
                    "no PaymentPolicy models the raw value " + raw.name());
        }
        for (PaymentPolicy domain : PaymentPolicy.values()) {
            assertDoesNotThrow(() -> PaymentPolicyEnum.fromValue(domain.name()),
                    "no raw PaymentPolicyEnum for the domain value " + domain.name());
        }
    }
}
