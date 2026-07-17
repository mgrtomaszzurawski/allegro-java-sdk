/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.contacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder.ContactRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast constraint coverage for the {@link ContactRequest}
 * builder. The Allegro contract marks no field required, so the failure tests
 * target the server-side limits the builder replicates (lengths, item counts)
 * rather than missing-required-field cases.
 */
class ContactRequestBuilderTest {

    private static final int NAME_MAX_LENGTH = 250;
    private static final int EMAIL_MAX_LENGTH = 128;
    private static final int PHONE_MAX_LENGTH = 250;

    private static final String TEST_NAME = "Main contact";
    private static final String TEST_EMAIL = "shop@example.com";
    private static final String TEST_PHONE_1 = "+48512323495";
    private static final String TEST_PHONE_2 = "+48512323496";
    private static final String TEST_PHONE_3 = "+48512323497";
    private static final String OTHER_EMAIL = "orders@example.com";
    private static final String REPLACEMENT_NAME = "Returns contact";
    private static final String FILLER = "a";

    @Test
    void build_whenNoFieldsSet_producesEmptyRequest() {
        // when
        ContactRequest request = ContactRequest.builder().build();

        // then — nothing is required, so an empty card is legal
        assertNull(request.name());
        assertTrue(request.emails().isEmpty());
        assertTrue(request.phones().isEmpty());
    }

    @Test
    void build_whenAllFieldsSet_capturesEveryValue() {
        // when
        ContactRequest request = ContactRequest.builder()
                .name(TEST_NAME)
                .email(TEST_EMAIL)
                .phone(TEST_PHONE_1)
                .phone(TEST_PHONE_2)
                .build();

        // then
        assertEquals(TEST_NAME, request.name());
        assertEquals(List.of(TEST_EMAIL), request.emails());
        assertEquals(List.of(TEST_PHONE_1, TEST_PHONE_2), request.phones());
    }

    @Test
    void toBuilder_whenNameChanged_preservesEmailAndPhones() {
        // given
        ContactRequest original = ContactRequest.builder()
                .name(TEST_NAME)
                .email(TEST_EMAIL)
                .phone(TEST_PHONE_1)
                .build();

        // when
        ContactRequest edited = original.toBuilder().name(REPLACEMENT_NAME).build();

        // then — only the name changed; the copied fields survived
        assertEquals(REPLACEMENT_NAME, edited.name());
        assertEquals(List.of(TEST_EMAIL), edited.emails());
        assertEquals(List.of(TEST_PHONE_1), edited.phones());
    }

    @Test
    void email_whenCalledTwice_keepsOnlyTheLastValue() {
        // when — the contract allows at most one e-mail
        ContactRequest request = ContactRequest.builder()
                .email(TEST_EMAIL)
                .email(OTHER_EMAIL)
                .build();

        // then
        assertEquals(List.of(OTHER_EMAIL), request.emails());
    }

    @Test
    void name_whenAtMaxLength_accepted() {
        // given
        String maxName = FILLER.repeat(NAME_MAX_LENGTH);

        // then
        assertEquals(maxName, ContactRequest.builder().name(maxName).build().name());
    }

    @Test
    void name_whenOverMaxLength_throwsIllegalArgument() {
        // given
        String tooLong = FILLER.repeat(NAME_MAX_LENGTH + 1);

        // then
        assertThrows(IllegalArgumentException.class,
                () -> ContactRequest.builder().name(tooLong));
    }

    @Test
    void email_whenAtMaxLength_accepted() {
        // given
        String maxEmail = FILLER.repeat(EMAIL_MAX_LENGTH);

        // then
        assertEquals(List.of(maxEmail), ContactRequest.builder().email(maxEmail).build().emails());
    }

    @Test
    void email_whenOverMaxLength_throwsIllegalArgument() {
        // given
        String tooLong = FILLER.repeat(EMAIL_MAX_LENGTH + 1);

        // then
        assertThrows(IllegalArgumentException.class,
                () -> ContactRequest.builder().email(tooLong));
    }

    @Test
    void phone_whenAtMaxLength_accepted() {
        // given
        String maxPhone = FILLER.repeat(PHONE_MAX_LENGTH);

        // then
        assertEquals(List.of(maxPhone), ContactRequest.builder().phone(maxPhone).build().phones());
    }

    @Test
    void phone_whenOverMaxLength_throwsIllegalArgument() {
        // given
        String tooLong = FILLER.repeat(PHONE_MAX_LENGTH + 1);

        // then
        assertThrows(IllegalArgumentException.class,
                () -> ContactRequest.builder().phone(tooLong));
    }

    @Test
    void phone_whenThirdAdded_throwsIllegalArgument() {
        // given — the contract allows at most two phone numbers
        ContactRequest.Builder builder = ContactRequest.builder()
                .phone(TEST_PHONE_1)
                .phone(TEST_PHONE_2);

        // then
        assertThrows(IllegalArgumentException.class, () -> builder.phone(TEST_PHONE_3));
    }

    @Test
    void name_whenNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> ContactRequest.builder().name(null));
    }

    @Test
    void email_whenNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> ContactRequest.builder().email(null));
    }

    @Test
    void phone_whenNull_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> ContactRequest.builder().phone(null));
    }
}
