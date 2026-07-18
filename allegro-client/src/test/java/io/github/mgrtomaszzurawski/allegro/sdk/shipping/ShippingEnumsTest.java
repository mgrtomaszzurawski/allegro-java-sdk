/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw.PaymentPolicyEnum;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.JoinStrategy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PaymentPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import org.junit.jupiter.api.Test;

/**
 * The point-of-service enums are fail-soft on read (unmodelled server values map
 * to {@code UNKNOWN}) and strict on write ({@code UNKNOWN} cannot be serialized).
 * {@link PaymentPolicy} is read-only and equally fail-soft; a parity test locks
 * that every real raw value still has a modelled counterpart (and vice versa), so
 * a spec regeneration that renames a value fails the build instead of silently
 * degrading a known policy to {@code UNKNOWN}.
 */
class ShippingEnumsTest {

    private static final String UNMODELLED = "PARCEL_LOCKER";

    @Test
    void fromWire_whenKnownValue_mapsToEnum() {
        assertEquals(PosType.PICKUP_POINT, PosType.fromWire("PICKUP_POINT"));
        assertEquals(PosStatus.TEMPORARILY_CLOSED, PosStatus.fromWire("TEMPORARILY_CLOSED"));
        assertEquals(ConfirmationType.AWAIT_CONTACT, ConfirmationType.fromWire("AWAIT_CONTACT"));
        assertEquals(JoinStrategy.SUM, JoinStrategy.fromWire("SUM"));
        assertEquals(RateSetType.PHYSICAL, RateSetType.fromWire("PHYSICAL"));
    }

    @Test
    void fromWire_whenUnmodelledValue_mapsToUnknown() {
        assertEquals(PosType.UNKNOWN, PosType.fromWire(UNMODELLED));
        assertEquals(PosStatus.UNKNOWN, PosStatus.fromWire(UNMODELLED));
        assertEquals(ConfirmationType.UNKNOWN, ConfirmationType.fromWire(UNMODELLED));
        assertEquals(JoinStrategy.UNKNOWN, JoinStrategy.fromWire(UNMODELLED));
        assertEquals(RateSetType.UNKNOWN, RateSetType.fromWire(UNMODELLED));
    }

    @Test
    void fromWire_whenNull_mapsToUnknown() {
        assertEquals(PosType.UNKNOWN, PosType.fromWire(null));
        assertEquals(PosStatus.UNKNOWN, PosStatus.fromWire(null));
        assertEquals(ConfirmationType.UNKNOWN, ConfirmationType.fromWire(null));
        assertEquals(JoinStrategy.UNKNOWN, JoinStrategy.fromWire(null));
        assertEquals(RateSetType.UNKNOWN, RateSetType.fromWire(null));
    }

    @Test
    void wireValue_whenKnownValue_isTheWireString() {
        assertEquals("PICKUP_POINT", PosType.PICKUP_POINT.wireValue());
        assertEquals("ACTIVE", PosStatus.ACTIVE.wireValue());
        assertEquals("CONTACT_NOT_REQUIRED", ConfirmationType.CONTACT_NOT_REQUIRED.wireValue());
        assertEquals("MIN", JoinStrategy.MIN.wireValue());
        assertEquals("ELECTRONIC", RateSetType.ELECTRONIC.wireValue());
    }

    @Test
    void wireValue_whenUnknown_throws() {
        assertThrows(IllegalStateException.class, PosType.UNKNOWN::wireValue);
        assertThrows(IllegalStateException.class, PosStatus.UNKNOWN::wireValue);
        assertThrows(IllegalStateException.class, ConfirmationType.UNKNOWN::wireValue);
        assertThrows(IllegalStateException.class, JoinStrategy.UNKNOWN::wireValue);
        assertThrows(IllegalStateException.class, RateSetType.UNKNOWN::wireValue);
    }

    @Test
    void paymentPolicyFromWire_whenUnmodelledOrNull_mapsToUnknown() {
        // Read fail-soft: a new server value (or the generator sentinel) must not
        // break the delivery-methods read — it degrades to UNKNOWN.
        assertEquals(PaymentPolicy.UNKNOWN, PaymentPolicy.fromWire(UNMODELLED));
        assertEquals(PaymentPolicy.UNKNOWN,
                PaymentPolicy.fromWire(PaymentPolicyEnum.UNKNOWN_DEFAULT_OPEN_API.name()));
        assertEquals(PaymentPolicy.UNKNOWN, PaymentPolicy.fromWire(null));
    }

    @Test
    void paymentPolicy_domainAndRawEnumsShareEveryName() {
        // DeliveryMethod maps PaymentPolicyEnum -> PaymentPolicy by name; this
        // fails the build if a future spec regeneration renames a value on one
        // side only, which would silently degrade a known policy to UNKNOWN.
        for (PaymentPolicyEnum raw : PaymentPolicyEnum.values()) {
            // The generator's forward-compat sentinel has no wire counterpart; it
            // is expected to map to UNKNOWN (asserted above), not to a real value.
            if (raw == PaymentPolicyEnum.UNKNOWN_DEFAULT_OPEN_API) {
                continue;
            }
            assertEquals(raw.name(), PaymentPolicy.fromWire(raw.name()).name(),
                    "no PaymentPolicy models the raw value " + raw.name());
        }
        for (PaymentPolicy domain : PaymentPolicy.values()) {
            // UNKNOWN is a domain-only read sentinel, not a wire value.
            if (domain == PaymentPolicy.UNKNOWN) {
                continue;
            }
            // fromValue never throws under enumUnknownDefaultCase (an unknown maps
            // to the sentinel), so assert it resolves to the matching real value.
            assertEquals(domain.name(), PaymentPolicyEnum.fromValue(domain.name()).name(),
                    "no raw PaymentPolicyEnum for the domain value " + domain.name());
        }
    }
}
