/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.junit.jupiter.api.Test;

/** Round-trip and fail-fast validation coverage for {@link ResponsiblePersonRequest}. */
class ResponsiblePersonRequestBuilderTest {

    private static final String NAME = "Person responsible for batteries";
    private static final String PERSON_NAME = "Responsible person company name";
    private static final int NAME_AT_LIMIT = 50;
    private static final int OVER_NAME_LIMIT = 51;
    private static final int OVER_PERSON_NAME_LIMIT = 201;
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
        ResponsiblePersonRequest request = ResponsiblePersonRequest.builder().name(NAME).build();

        assertEquals(NAME, request.name());
        assertNull(request.personName());
        assertNull(request.address());
        assertNull(request.contact());
    }

    @Test
    void build_whenAllFieldsSet_preservesEveryField() {
        ResponsiblePersonRequest request = ResponsiblePersonRequest.builder()
                .name(NAME)
                .personName(PERSON_NAME)
                .address(address())
                .contact(contact())
                .build();

        assertEquals(NAME, request.name());
        assertEquals(PERSON_NAME, request.personName());
        assertEquals("Warszawa", request.address().city());
        assertEquals("some@email.com", request.contact().email());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        ResponsiblePersonRequest original = ResponsiblePersonRequest.builder()
                .name(NAME)
                .personName(PERSON_NAME)
                .address(address())
                .contact(contact())
                .build();

        ResponsiblePersonRequest copy = original.toBuilder().build();

        assertEquals(original.name(), copy.name());
        assertEquals(original.personName(), copy.personName());
        assertEquals(original.address().street(), copy.address().street());
        assertEquals(original.contact().phoneNumber(), copy.contact().phoneNumber());
    }

    @Test
    void build_whenNameMissing_throws() {
        var builder = ResponsiblePersonRequest.builder().personName(PERSON_NAME);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenNameTooLong_throws() {
        var builder = ResponsiblePersonRequest.builder().name("a".repeat(OVER_NAME_LIMIT));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }

    @Test
    void build_whenNameAtLimit_succeeds() {
        String maxName = "a".repeat(NAME_AT_LIMIT);
        ResponsiblePersonRequest request = ResponsiblePersonRequest.builder().name(maxName).build();
        assertEquals(maxName, request.name());
    }

    @Test
    void build_whenPersonNameTooLong_throws() {
        var builder = ResponsiblePersonRequest.builder()
                .name(NAME)
                .personName("a".repeat(OVER_PERSON_NAME_LIMIT));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }

    @Test
    void build_whenContactHasNeitherEmailNorFormUrl_throws() {
        var builder = ResponsiblePersonRequest.builder()
                .name(NAME)
                .contact(new ResponsiblePartyContact(null, "123123123", null));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains("email"));
    }
}
