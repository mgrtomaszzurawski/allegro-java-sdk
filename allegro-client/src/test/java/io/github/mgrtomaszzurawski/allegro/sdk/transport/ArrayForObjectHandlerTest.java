/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ArrayForObjectHandler;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Pins the array-for-object tolerance. The vendored spec types
 * {@code SaleProductOfferResponseV1Raw.warnings} as a single object, but the live
 * sandbox returns it as an array (always {@code []} on a valid offer, captured
 * 2026-07-21 on offer {@code 7781898446}) — which alone broke every
 * {@code getProductOffer} read with a Jackson {@code MismatchedInputException}. The
 * handler consumes the unexpected array (empty → {@code null}, else first element)
 * so an otherwise-good response deserializes.
 */
class ArrayForObjectHandlerTest {

    private static final String OFFER_EMPTY_WARNINGS_ARRAY =
            "{\"name\":\"x\",\"warnings\":[]}";
    private static final String OFFER_NON_EMPTY_WARNINGS_ARRAY =
            "{\"name\":\"x\",\"warnings\":[{},{}]}";
    private static final String ARRAY_VALUE_MARKER = "from Array value";

    /** The SDK mapper config WITHOUT the tolerance handler. */
    private static ObjectMapper lenientMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static ObjectMapper tolerantMapper() {
        return lenientMapper().addHandler(new ArrayForObjectHandler());
    }

    @Test
    void deserialize_whenObjectFieldIsArrayWithoutHandler_fails() {
        // given — the lenient mapper without the array-for-object handler
        ObjectMapper lenient = lenientMapper();
        // when / then — the array where an object is expected fails the whole read with the
        // structural mismatch Jackson raises (assert the type, not the version-sensitive message)
        MismatchedInputException thrown = assertThrows(MismatchedInputException.class, () ->
                lenient.readValue(OFFER_EMPTY_WARNINGS_ARRAY, SaleProductOfferResponseV1Raw.class));
        assertTrue(thrown.getMessage().contains(ARRAY_VALUE_MARKER), thrown.getMessage());
    }

    @Test
    void deserialize_whenObjectFieldIsEmptyArray_withHandlerYieldsNull() throws Exception {
        // given the tolerant mapper; when reading an offer whose `warnings` is []
        SaleProductOfferResponseV1Raw raw = tolerantMapper()
                .readValue(OFFER_EMPTY_WARNINGS_ARRAY, SaleProductOfferResponseV1Raw.class);
        // then the rest of the offer deserializes and the array field is null
        assertEquals("x", raw.getName());
        assertNull(raw.getWarnings());
    }

    @Test
    void deserialize_whenObjectFieldIsNonEmptyArray_withHandlerTakesFirstElement() throws Exception {
        // given the tolerant mapper; when reading an offer whose `warnings` is [{},{}]
        SaleProductOfferResponseV1Raw raw = tolerantMapper()
                .readValue(OFFER_NON_EMPTY_WARNINGS_ARRAY, SaleProductOfferResponseV1Raw.class);
        // then the first element is taken (later elements dropped) and the read succeeds
        assertEquals("x", raw.getName());
        assertNotNull(raw.getWarnings());
    }
}
