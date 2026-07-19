/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerContactRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.junit.jupiter.api.Test;

/**
 * Raw mapping for {@link ResponsiblePartyContact}. The record is a plain value
 * type — the email-or-formUrl write rule lives in the request builders, so
 * response mapping accepts whatever the server sends.
 */
class ResponsiblePartyContactTest {

    private static final String EMAIL = "some@email.com";
    private static final String PHONE = "123123123";
    private static final String FORM_URL = "https://example.com/contact";

    @Test
    void constructor_whenEmailOnly_leavesFormUrlNull() {
        ResponsiblePartyContact contact = new ResponsiblePartyContact(EMAIL, PHONE, null);
        assertEquals(EMAIL, contact.email());
        assertEquals(PHONE, contact.phoneNumber());
        assertNull(contact.formUrl());
    }

    @Test
    void constructor_whenFormUrlOnly_leavesEmailNull() {
        ResponsiblePartyContact contact = new ResponsiblePartyContact(null, null, FORM_URL);
        assertEquals(FORM_URL, contact.formUrl());
        assertNull(contact.email());
    }

    @Test
    void constructor_whenPhoneOnly_constructs() {
        // the record does not enforce the write rule, so a read never rejects a payload
        ResponsiblePartyContact contact = new ResponsiblePartyContact(null, PHONE, null);
        assertEquals(PHONE, contact.phoneNumber());
        assertNull(contact.email());
        assertNull(contact.formUrl());
    }

    @Test
    void from_whenPersonContactRaw_mapsEveryField() {
        ResponsiblePersonContactRaw raw = new ResponsiblePersonContactRaw()
                .email(EMAIL).phoneNumber(PHONE).formUrl(FORM_URL);

        ResponsiblePartyContact contact = ResponsiblePartyContact.from(raw);

        assertEquals(EMAIL, contact.email());
        assertEquals(PHONE, contact.phoneNumber());
        assertEquals(FORM_URL, contact.formUrl());
    }

    @Test
    void from_whenProducerContactRaw_mapsEveryField() {
        ResponsibleProducerContactRaw raw = new ResponsibleProducerContactRaw()
                .email(EMAIL).phoneNumber(PHONE).formUrl(FORM_URL);

        ResponsiblePartyContact contact = ResponsiblePartyContact.from(raw);

        assertEquals(EMAIL, contact.email());
        assertEquals(FORM_URL, contact.formUrl());
    }

    @Test
    void from_whenNullPersonRaw_returnsNull() {
        assertNull(ResponsiblePartyContact.from((ResponsiblePersonContactRaw) null));
    }
}
