/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.junit.jupiter.api.Test;

/** Round-trip and fail-fast validation coverage for {@link ResponsibleProducerRequest}. */
class ResponsibleProducerRequestBuilderTest {

    private static final String NAME = "Company responsible for producing product";
    private static final String TRADE_NAME = "Trade name of responsible producer";
    private static final int NAME_AT_LIMIT = 50;
    private static final int OVER_NAME_LIMIT = 51;
    private static final int OVER_TRADE_NAME_LIMIT = 201;
    private static final String FIELD_NAME = "name";
    private static final String LENGTH_WORD = "exceeds";

    private static ResponsiblePartyAddress address() {
        return new ResponsiblePartyAddress("PL", "Wiśniowa 1", "00-000", "Warszawa");
    }

    private static ResponsiblePartyContact contact() {
        return new ResponsiblePartyContact("some@email.com", "123123123", null);
    }

    @Test
    void build_whenRequiredFieldsOnly_succeeds() {
        ResponsibleProducerRequest request = ResponsibleProducerRequest.builder().name(NAME).build();

        assertEquals(NAME, request.name());
        assertNull(request.tradeName());
        assertNull(request.address());
        assertNull(request.contact());
    }

    @Test
    void build_whenAllFieldsSet_preservesEveryField() {
        ResponsibleProducerRequest request = ResponsibleProducerRequest.builder()
                .name(NAME)
                .tradeName(TRADE_NAME)
                .address(address())
                .contact(contact())
                .build();

        assertEquals(NAME, request.name());
        assertEquals(TRADE_NAME, request.tradeName());
        assertEquals("Warszawa", request.address().city());
        assertEquals("some@email.com", request.contact().email());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        ResponsibleProducerRequest original = ResponsibleProducerRequest.builder()
                .name(NAME)
                .tradeName(TRADE_NAME)
                .address(address())
                .contact(contact())
                .build();

        ResponsibleProducerRequest copy = original.toBuilder().build();

        assertEquals(original.name(), copy.name());
        assertEquals(original.tradeName(), copy.tradeName());
        assertEquals(original.address().postalCode(), copy.address().postalCode());
        assertEquals(original.contact().email(), copy.contact().email());
    }

    @Test
    void build_whenNameMissing_throws() {
        var builder = ResponsibleProducerRequest.builder().tradeName(TRADE_NAME);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenNameTooLong_throws() {
        var builder = ResponsibleProducerRequest.builder().name("a".repeat(OVER_NAME_LIMIT));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }

    @Test
    void build_whenNameAtLimit_succeeds() {
        String maxName = "a".repeat(NAME_AT_LIMIT);
        ResponsibleProducerRequest request = ResponsibleProducerRequest.builder().name(maxName).build();
        assertEquals(maxName, request.name());
    }

    @Test
    void build_whenTradeNameTooLong_throws() {
        var builder = ResponsibleProducerRequest.builder()
                .name(NAME)
                .tradeName("a".repeat(OVER_TRADE_NAME_LIMIT));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }
}
