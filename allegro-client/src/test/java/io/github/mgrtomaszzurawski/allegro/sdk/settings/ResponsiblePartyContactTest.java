/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerContactRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.junit.jupiter.api.Test;

/** Construction-time validation and Raw mapping for {@link ResponsiblePartyContact}. */
class ResponsiblePartyContactTest {

    private static final String EMAIL = "some@email.com";
    private static final String PHONE = "123123123";
    private static final String FORM_URL = "https://example.com/contact";

    @Test
    void constructor_whenEmailOnly_succeeds() {
        ResponsiblePartyContact contact = new ResponsiblePartyContact(EMAIL, PHONE, null);
        assertEquals(EMAIL, contact.email());
        assertEquals(PHONE, contact.phoneNumber());
    }

    @Test
    void constructor_whenFormUrlOnly_succeeds() {
        ResponsiblePartyContact contact = new ResponsiblePartyContact(null, null, FORM_URL);
        assertEquals(FORM_URL, contact.formUrl());
    }

    @Test
    void constructor_whenNeitherEmailNorFormUrl_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResponsiblePartyContact(null, PHONE, null));
    }

    @Test
    void constructor_whenEmailAndFormUrlBlank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResponsiblePartyContact("  ", PHONE, "  "));
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
