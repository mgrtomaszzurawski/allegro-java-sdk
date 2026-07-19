/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.client.model.AlreadyInWarehouseShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CourierBySellerShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingExtendedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.StrictOneOfModule;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.UnknownSubtypeToBaseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Pins a Layer-1 generation defect that blocks the Advance Ship Notice
 * {@code shipping} declaration, and documents the executable evidence behind the
 * bucket-I deferral of that feature.
 *
 * <p><b>The defect.</b> The ASN read DTOs type their {@code shipping} field as
 * {@code ShippingExtendedRaw} (the spec's {@code ShippingExtended}, four methods
 * including the read-only {@code ALREADY_IN_WAREHOUSE}). The generator (OpenAPI
 * Generator 7.12.0, {@code native} library) emits the three shared write methods —
 * {@code COURIER_BY_SELLER}, {@code OWN_TRANSPORT}, {@code THIRD_PARTY_DELIVERY} —
 * as classes extending {@code ShippingRaw} (the write base {@code Shipping}), NOT
 * {@code ShippingExtendedRaw}, because their {@code allOf} lists {@code Shipping}
 * first. {@code ShippingExtendedRaw} still lists those classes in its
 * {@code @JsonSubTypes}, so Jackson resolves the {@code method} to a class that is
 * not a subtype of the field type and throws {@link InvalidTypeIdException}, failing
 * the WHOLE ASN response. Only {@code ALREADY_IN_WAREHOUSE} (which does extend
 * {@code ShippingExtendedRaw}) reads. The core {@code UnknownSubtypeToBaseHandler}
 * does not help: it rescues an <i>unknown</i> type id, whereas here the id is known
 * and resolves — to a wrong-parent class.
 *
 * <p><b>Consequence.</b> Any real ASN carrying courier / own-transport /
 * third-party shipping cannot be read, and {@code create} / {@code updateSubmitted}
 * echo {@code ShippingExtendedRaw} too, so even a write cannot round-trip. The
 * {@code shipping} declaration therefore stays deferred (see {@code CHANGELOG} I and
 * {@code docs/fulfillment.md}) until Layer-1 is regenerated so the three methods
 * extend {@code ShippingExtendedRaw}. That fix is owned by the build/core agent
 * (Layer-1 is frozen for domain buckets) — tracked in the shared {@code BACKLOG}.
 *
 * <p><b>Signal.</b> When Layer-1 is fixed, {@link #readExtendedShipping_whenSharedWriteMethod_throwsUntilLayer1Fixed}
 * flips from passing to failing — that is the cue to build the domain shipping model
 * and wire it into the ASN read/write paths. The write base {@code ShippingRaw} is
 * already sound, as {@link #readWriteBaseShipping_whenCourierBySeller_deserializes}
 * shows, so the write mapping needs no Layer-1 change.
 */
class AsnShippingLayer1RegressionTest {

    private static final String METHOD_COURIER = "COURIER_BY_SELLER";
    private static final String METHOD_OWN_TRANSPORT = "OWN_TRANSPORT";
    private static final String METHOD_THIRD_PARTY = "THIRD_PARTY_DELIVERY";
    private static final String NOT_SUBTYPE_MARKER = "not subtype";
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

    /** The SDK's response ObjectMapper configuration, including the core forward-compat handler. */
    private ObjectMapper sdkMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .registerModule(new StrictOneOfModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .addHandler(new UnknownSubtypeToBaseHandler());
    }

    @ParameterizedTest
    @ValueSource(strings = {METHOD_COURIER, METHOD_OWN_TRANSPORT, METHOD_THIRD_PARTY})
    void readExtendedShipping_whenSharedWriteMethod_throwsUntilLayer1Fixed(String method) throws Exception {
        // given: a ShippingExtended payload for one of the three shared write methods
        String json = switch (method) {
            case METHOD_COURIER -> COURIER_JSON;
            case METHOD_OWN_TRANSPORT -> OWN_TRANSPORT_JSON;
            default -> THIRD_PARTY_JSON;
        };
        ObjectMapper mapper = sdkMapper();

        // when / then: the generated subtype is not a ShippingExtendedRaw, so read fails fast
        InvalidTypeIdException failure = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, ShippingExtendedRaw.class));
        assertTrue(failure.getMessage().contains(NOT_SUBTYPE_MARKER),
                () -> "expected a not-subtype resolution failure but got: " + failure.getMessage());
    }

    @Test
    void readExtendedShipping_whenAlreadyInWarehouse_deserializes() throws Exception {
        // given: the one method whose generated subtype does extend ShippingExtendedRaw
        ObjectMapper mapper = sdkMapper();

        // when
        ShippingExtendedRaw raw = mapper.readValue(WAREHOUSE_JSON, ShippingExtendedRaw.class);

        // then
        assertEquals(AlreadyInWarehouseShippingRaw.class, raw.getClass());
        assertEquals(COUNTRY_PL, ((AlreadyInWarehouseShippingRaw) raw).getCountryCode());
    }

    @Test
    void readWriteBaseShipping_whenCourierBySeller_deserializes() throws Exception {
        // given: the write base ShippingRaw, whose subtypes are correctly generated
        ObjectMapper mapper = sdkMapper();

        // when
        ShippingRaw raw = mapper.readValue(COURIER_JSON, ShippingRaw.class);

        // then: the write path is sound and needs no Layer-1 change
        assertEquals(CourierBySellerShippingRaw.class, raw.getClass());
        assertEquals(COUNTRY_PL, ((CourierBySellerShippingRaw) raw).getCountryCode());
    }
}
