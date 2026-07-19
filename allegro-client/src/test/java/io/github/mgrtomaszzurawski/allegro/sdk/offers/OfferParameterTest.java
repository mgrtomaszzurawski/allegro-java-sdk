/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRangeValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ParameterRange;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferParameterTest {

    private static final String PARAM_ID = "11321";
    private static final String PARAM_NAME = "Color";
    private static final String VALUE_ID_ONE = "1";
    private static final String VALUE_ID_TWO = "2";
    private static final String FREE_VALUE = "custom";
    private static final String RANGE_FROM = "10";
    private static final String RANGE_TO = "20";

    @Test
    void dictionary_whenBuilt_carriesOnlyValueIds() {
        OfferParameter parameter = OfferParameter.dictionary(PARAM_ID, VALUE_ID_ONE, VALUE_ID_TWO);

        assertEquals(PARAM_ID, parameter.id());
        assertEquals(List.of(VALUE_ID_ONE, VALUE_ID_TWO), parameter.valuesIds());
        assertEquals(List.of(), parameter.values());
        assertNull(parameter.rangeValue());
        assertNull(parameter.name());
    }

    @Test
    void freeText_whenBuilt_carriesOnlyValues() {
        OfferParameter parameter = OfferParameter.freeText(PARAM_ID, FREE_VALUE);

        assertEquals(List.of(FREE_VALUE), parameter.values());
        assertEquals(List.of(), parameter.valuesIds());
        assertNull(parameter.rangeValue());
    }

    @Test
    void range_whenBuilt_carriesOnlyRange() {
        OfferParameter parameter = OfferParameter.range(PARAM_ID, RANGE_FROM, RANGE_TO);

        assertEquals(new ParameterRange(RANGE_FROM, RANGE_TO), parameter.rangeValue());
        assertEquals(List.of(), parameter.values());
        assertEquals(List.of(), parameter.valuesIds());
    }

    @Test
    void dictionary_whenSerialized_writesIdAndValueIdsOnly() {
        ParameterProductOfferRequestRaw raw = OfferParameter.dictionary(PARAM_ID, VALUE_ID_ONE).toRaw();

        assertEquals(PARAM_ID, raw.getId());
        assertEquals(List.of(VALUE_ID_ONE), raw.getValuesIds());
        assertTrue(raw.getValues() == null || raw.getValues().isEmpty());
        assertNull(raw.getRangeValue());
    }

    @Test
    void range_whenSerialized_writesRangeValue() {
        ParameterProductOfferRequestRaw raw = OfferParameter.range(PARAM_ID, RANGE_FROM, RANGE_TO).toRaw();

        assertEquals(RANGE_FROM, raw.getRangeValue().getFrom());
        assertEquals(RANGE_TO, raw.getRangeValue().getTo());
    }

    @Test
    void from_whenAllValueKindsAbsent_degradesToEmptyAndNull() {
        // given — a response parameter with an id/name but no value lists or range
        // (the value lists are absent on the wire, the range unset)
        ParameterProductOfferResponseRaw raw =
                new ParameterProductOfferResponseRaw().id(PARAM_ID).name(PARAM_NAME).values(null).valuesIds(null);

        // when
        OfferParameter parameter = OfferParameter.from(raw);

        // then — absent lists become empty, the absent range becomes null, no NPE
        assertEquals(PARAM_NAME, parameter.name());
        assertEquals(List.of(), parameter.values());
        assertEquals(List.of(), parameter.valuesIds());
        assertNull(parameter.rangeValue());
    }

    @Test
    void from_whenRangePresent_mapsBothBounds() {
        ParameterProductOfferResponseRaw raw = new ParameterProductOfferResponseRaw()
                .id(PARAM_ID)
                .rangeValue(new ParameterRangeValueRaw().from(RANGE_FROM).to(RANGE_TO));

        OfferParameter parameter = OfferParameter.from(raw);

        assertEquals(new ParameterRange(RANGE_FROM, RANGE_TO), parameter.rangeValue());
    }

    @Test
    void dictionary_whenRoundTripped_preservesValueIds() {
        // given — a dictionary parameter serialized to the request shape, then read back
        // from the equivalent response shape (values are named the same on both DTOs)
        ParameterProductOfferRequestRaw requestRaw =
                OfferParameter.dictionary(PARAM_ID, VALUE_ID_ONE, VALUE_ID_TWO).toRaw();
        ParameterProductOfferResponseRaw responseRaw = new ParameterProductOfferResponseRaw()
                .id(requestRaw.getId()).valuesIds(requestRaw.getValuesIds());

        // when
        OfferParameter roundTripped = OfferParameter.from(responseRaw);

        // then — id and value ids survive the round trip
        assertEquals(PARAM_ID, roundTripped.id());
        assertEquals(List.of(VALUE_ID_ONE, VALUE_ID_TWO), roundTripped.valuesIds());
    }

    @Test
    void parameterRange_whenBoundNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> ParameterRange.of(null, RANGE_TO));
        assertThrows(NullPointerException.class, () -> ParameterRange.of(RANGE_FROM, null));
    }

    @Test
    void constructor_whenIdNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> OfferParameter.dictionary(null, VALUE_ID_ONE));
    }
}
