/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.client.model.AlreadyInWarehouseShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CourierBySellerShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OwnTransportShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThirdPartyDeliveryShippingRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.StrictOneOfModule;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.UnknownSubtypeToBaseHandler;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Guards the Layer-1 shipping hierarchy behind the Advance Ship Notice {@code shipping} field.
 *
 * <p><b>The former defect (fixed).</b> The ASN read DTOs typed {@code shipping} as
 * {@code ShippingExtended} while the three shared write methods — {@code COURIER_BY_SELLER},
 * {@code OWN_TRANSPORT}, {@code THIRD_PARTY_DELIVERY} — generated as classes extending the write
 * base {@code Shipping}, not {@code ShippingExtended}. Jackson resolved those methods to
 * non-subtype classes and threw {@code InvalidTypeIdException}, failing the WHOLE ASN response.
 *
 * <p><b>The fix.</b> The generation-time spec patch collapses {@code ShippingExtended} into a
 * single {@code Shipping} discriminated base carrying all four methods (the read-only
 * {@code ALREADY_IN_WAREHOUSE} included); every ASN {@code shipping} field is typed
 * {@code ShippingRaw}. This test pins that every method now deserializes to its concrete subtype
 * through the SDK's response {@code ObjectMapper}; a Layer-1 regeneration that reintroduced the
 * split would fail it. The write base was always sound, so writes round-trip unchanged.
 */
class AsnShippingLayer1RegressionTest {

    private static final String COUNTRY_PL = "PL";

    private static final String COURIER_JSON =
            "{\"method\":\"COURIER_BY_SELLER\",\"courier\":{\"id\":\"DPD\"},"
            + "\"estimatedTimeOfArrival\":\"2020-08-26T12:50:04Z\",\"countryCode\":\"PL\"}";
    private static final String OWN_TRANSPORT_JSON =
            "{\"method\":\"OWN_TRANSPORT\",\"truckLicencePlate\":\"FZ12453\","
            + "\"estimatedTimeOfArrival\":\"2020-08-26T12:50:04Z\",\"countryCode\":\"PL\"}";
    private static final String THIRD_PARTY_JSON =
            "{\"method\":\"THIRD_PARTY_DELIVERY\",\"thirdParty\":{\"name\":\"Company ABC\"},"
            + "\"estimatedTimeOfArrival\":\"2020-08-26T12:50:04Z\",\"countryCode\":\"PL\"}";
    private static final String WAREHOUSE_JSON =
            "{\"method\":\"ALREADY_IN_WAREHOUSE\","
            + "\"estimatedTimeOfArrival\":\"2020-08-26T12:50:04Z\",\"countryCode\":\"PL\"}";

    /**
     * The SDK's response ObjectMapper configuration, including the core forward-compat handler.
     * Kept module-for-module in step with {@code AllegroClient}'s inline mapper; there is no
     * extractable factory to share (that would be a frozen-core change owned by the build/core
     * agent), so this mirror must be updated if that configuration changes.
     */
    private ObjectMapper sdkMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .registerModule(new StrictOneOfModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .addHandler(new UnknownSubtypeToBaseHandler());
    }

    private static Stream<Arguments> shippingMethods() {
        return Stream.of(
                arguments(COURIER_JSON, CourierBySellerShippingRaw.class),
                arguments(OWN_TRANSPORT_JSON, OwnTransportShippingRaw.class),
                arguments(THIRD_PARTY_JSON, ThirdPartyDeliveryShippingRaw.class),
                arguments(WAREHOUSE_JSON, AlreadyInWarehouseShippingRaw.class));
    }

    @ParameterizedTest
    @MethodSource("shippingMethods")
    void readShipping_deserializesEveryMethodToItsConcreteSubtype(String json, Class<?> expectedType)
            throws Exception {
        // given: an ASN shipping payload for one of the four methods
        // when: read through the field type used by every ASN DTO (ShippingRaw)
        ShippingRaw raw = sdkMapper().readValue(json, ShippingRaw.class);

        // then: the method discriminator resolves to the concrete subtype (no InvalidTypeIdException)
        assertEquals(expectedType, raw.getClass());
    }

    @Test
    void readShipping_whenCourierBySeller_populatesTheBody() throws Exception {
        // given: a COURIER_BY_SELLER payload
        // when
        ShippingRaw raw = sdkMapper().readValue(COURIER_JSON, ShippingRaw.class);

        // then: the concrete subtype's own fields are populated, not just the discriminator
        CourierBySellerShippingRaw courier = (CourierBySellerShippingRaw) raw;
        assertEquals(COUNTRY_PL, courier.getCountryCode());
    }
}
