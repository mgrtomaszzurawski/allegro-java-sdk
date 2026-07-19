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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameterKind;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ParameterRange;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferParameterTest {

    private static final String PARAM_ID = "11321";
    private static final String PARAM_NAME = "Color";
    private static final String VALUE_ID_ONE = "1";
    private static final String VALUE_ID_TWO = "2";
    private static final String VALUE_LABEL = "New";
    private static final String FREE_VALUE = "custom";
    private static final String FREE_VALUE_TWO = "Cherry MX";
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
        assertEquals(OfferParameterKind.DICTIONARY, parameter.kind());
    }

    @Test
    void freeText_whenBuilt_carriesOnlyValues() {
        OfferParameter parameter = OfferParameter.freeText(PARAM_ID, FREE_VALUE);

        assertEquals(List.of(FREE_VALUE), parameter.values());
        assertEquals(List.of(), parameter.valuesIds());
        assertNull(parameter.rangeValue());
        assertEquals(OfferParameterKind.FREE_TEXT, parameter.kind());
    }

    @Test
    void range_whenBuilt_carriesOnlyRange() {
        OfferParameter parameter = OfferParameter.range(PARAM_ID, RANGE_FROM, RANGE_TO);

        assertEquals(new ParameterRange(RANGE_FROM, RANGE_TO), parameter.rangeValue());
        assertEquals(List.of(), parameter.values());
        assertEquals(List.of(), parameter.valuesIds());
        assertEquals(OfferParameterKind.RANGE, parameter.kind());
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
    void freeText_whenSerialized_writesValuesOnly() {
        ParameterProductOfferRequestRaw raw =
                OfferParameter.freeText(PARAM_ID, FREE_VALUE, FREE_VALUE_TWO).toRaw();

        assertEquals(PARAM_ID, raw.getId());
        assertEquals(List.of(FREE_VALUE, FREE_VALUE_TWO), raw.getValues());
        assertTrue(raw.getValuesIds() == null || raw.getValuesIds().isEmpty());
        assertNull(raw.getRangeValue());
    }

    @Test
    void range_whenSerialized_writesRangeValue() {
        ParameterProductOfferRequestRaw raw = OfferParameter.range(PARAM_ID, RANGE_FROM, RANGE_TO).toRaw();

        assertEquals(RANGE_FROM, raw.getRangeValue().getFrom());
        assertEquals(RANGE_TO, raw.getRangeValue().getTo());
    }

    @Test
    void toRaw_whenNameSet_omitsNameBecauseItIsReadOnly() {
        // given — a parameter carrying a name (as a read-back one does), built via the
        // canonical constructor since the write factories leave the name null
        OfferParameter named =
                new OfferParameter(PARAM_ID, PARAM_NAME, List.of(), List.of(VALUE_ID_ONE), null);

        // then — the read-only name is not sent on write (the server keys on the id); only
        // the id and the value ids reach the request
        ParameterProductOfferRequestRaw raw = named.toRaw();
        assertNull(raw.getName());
        assertEquals(PARAM_ID, raw.getId());
        assertEquals(List.of(VALUE_ID_ONE), raw.getValuesIds());
    }

    @Test
    void from_whenAllValueKindsAbsent_degradesToEmptyAndNull() {
        // given — a response parameter with an id/name but no value lists or range
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
    void from_whenFreeTextValuesPresent_mapsValues() {
        // given — a free-text parameter read back with a populated values list and no ids
        ParameterProductOfferResponseRaw raw = new ParameterProductOfferResponseRaw()
                .id(PARAM_ID).values(List.of(FREE_VALUE));

        // when
        OfferParameter parameter = OfferParameter.from(raw);

        // then — the values map through and the kind is free-text
        assertEquals(List.of(FREE_VALUE), parameter.values());
        assertEquals(List.of(), parameter.valuesIds());
        assertEquals(OfferParameterKind.FREE_TEXT, parameter.kind());
    }

    @Test
    void from_whenDictionaryCarriesLabelsAndIds_keepsBothAndKindIsDictionary() {
        // given — the REAL wire shape of a dictionary parameter on read: both the value
        // ids AND their human-readable labels are populated together
        ParameterProductOfferResponseRaw raw = new ParameterProductOfferResponseRaw()
                .id(PARAM_ID).name(PARAM_NAME)
                .values(List.of(VALUE_LABEL))
                .valuesIds(List.of(VALUE_ID_ONE));

        // when
        OfferParameter parameter = OfferParameter.from(raw);

        // then — both lists are preserved, and kind() resolves to DICTIONARY (not free-text)
        assertEquals(List.of(VALUE_LABEL), parameter.values());
        assertEquals(List.of(VALUE_ID_ONE), parameter.valuesIds());
        assertEquals(OfferParameterKind.DICTIONARY, parameter.kind());
    }

    @Test
    void toRaw_whenDictionaryReadBack_sendsIdsOnlyNotLabels() {
        // given — a dictionary parameter as read from an offer, carrying both labels and ids
        OfferParameter readBack = OfferParameter.from(new ParameterProductOfferResponseRaw()
                .id(PARAM_ID).name(PARAM_NAME).values(List.of(VALUE_LABEL)).valuesIds(List.of(VALUE_ID_ONE)));

        // when — the same value is fed straight back into a create request
        ParameterProductOfferRequestRaw raw = readBack.toRaw();

        // then — only the value ids are written; the read-only labels are dropped so
        // Allegro does not reject the write with InvalidDictionaryParameter
        assertEquals(List.of(VALUE_ID_ONE), raw.getValuesIds());
        assertTrue(raw.getValues() == null || raw.getValues().isEmpty());
    }

    @Test
    void from_whenRangePresent_mapsBothBounds() {
        ParameterProductOfferResponseRaw raw = new ParameterProductOfferResponseRaw()
                .id(PARAM_ID)
                .rangeValue(new ParameterRangeValueRaw().from(RANGE_FROM).to(RANGE_TO));

        OfferParameter parameter = OfferParameter.from(raw);

        assertEquals(new ParameterRange(RANGE_FROM, RANGE_TO), parameter.rangeValue());
        assertEquals(OfferParameterKind.RANGE, parameter.kind());
    }

    @Test
    void dictionary_whenRoundTripped_preservesValueIds() {
        // given — a dictionary parameter serialized to the request shape, then read back
        // from the equivalent response shape (value ids are named the same on both DTOs)
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
    void freeText_whenRoundTripped_preservesValues() {
        ParameterProductOfferRequestRaw requestRaw =
                OfferParameter.freeText(PARAM_ID, FREE_VALUE).toRaw();
        ParameterProductOfferResponseRaw responseRaw = new ParameterProductOfferResponseRaw()
                .id(requestRaw.getId()).values(requestRaw.getValues());

        OfferParameter roundTripped = OfferParameter.from(responseRaw);

        assertEquals(List.of(FREE_VALUE), roundTripped.values());
        assertEquals(OfferParameterKind.FREE_TEXT, roundTripped.kind());
    }

    @Test
    void range_whenRoundTripped_preservesBounds() {
        ParameterProductOfferRequestRaw requestRaw =
                OfferParameter.range(PARAM_ID, RANGE_FROM, RANGE_TO).toRaw();
        ParameterProductOfferResponseRaw responseRaw = new ParameterProductOfferResponseRaw()
                .id(requestRaw.getId()).rangeValue(requestRaw.getRangeValue());

        OfferParameter roundTripped = OfferParameter.from(responseRaw);

        assertEquals(new ParameterRange(RANGE_FROM, RANGE_TO), roundTripped.rangeValue());
    }

    @Test
    void dictionary_whenNoValueIds_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OfferParameter.dictionary(PARAM_ID));
        assertThrows(IllegalArgumentException.class, () -> OfferParameter.dictionary(PARAM_ID, List.of()));
    }

    @Test
    void freeText_whenNoValues_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OfferParameter.freeText(PARAM_ID));
        assertThrows(IllegalArgumentException.class, () -> OfferParameter.freeText(PARAM_ID, List.of()));
    }

    @Test
    void parameterRange_whenBoundNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> ParameterRange.of(null, RANGE_TO));
        assertThrows(NullPointerException.class, () -> ParameterRange.of(RANGE_FROM, null));
    }

    @Test
    void dictionary_whenIdNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> OfferParameter.dictionary(null, VALUE_ID_ONE));
    }
}
