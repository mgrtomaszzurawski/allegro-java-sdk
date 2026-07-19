/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.DeliveryAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.InvoiceAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.InvoicePerson;
import org.junit.jupiter.api.Test;

/**
 * The order records that carry a person's data must keep it out of
 * {@code toString()} (so an accidental log or trace never leaks it) while still
 * exposing it through the typed accessors.
 */
class OrderPiiRedactionTest {

    private static final String FIRST_NAME = "Anna";
    private static final String LAST_NAME = "Nowak";
    private static final String STREET = "Main 5";
    private static final String CITY = "Warsaw";
    private static final String ZIP_CODE = "00-001";
    private static final String COUNTRY_CODE = "PL";
    private static final String PHONE = "+48500100200";

    @Test
    void toString_whenDeliveryAddress_redactsPersonalDataButAccessorsExposeIt() {
        // given
        DeliveryAddress address = new DeliveryAddress(
                FIRST_NAME, LAST_NAME, STREET, CITY, ZIP_CODE, COUNTRY_CODE, null, PHONE);

        // when
        String rendered = address.toString();

        // then — name, street and phone are absent from toString...
        assertFalse(rendered.contains(FIRST_NAME));
        assertFalse(rendered.contains(LAST_NAME));
        assertFalse(rendered.contains(STREET));
        assertFalse(rendered.contains(PHONE));
        // ...but still readable through the accessors
        assertEquals(FIRST_NAME, address.firstName());
        assertEquals(STREET, address.street());
        assertEquals(PHONE, address.phoneNumber());
    }

    @Test
    void toString_whenInvoicePerson_redactsNameButAccessorsExposeIt() {
        // given
        InvoicePerson person = new InvoicePerson(FIRST_NAME, LAST_NAME);

        // when
        String rendered = person.toString();

        // then
        assertFalse(rendered.contains(FIRST_NAME));
        assertFalse(rendered.contains(LAST_NAME));
        assertEquals(FIRST_NAME, person.firstName());
        assertEquals(LAST_NAME, person.lastName());
    }

    @Test
    void toString_whenInvoiceAddress_redactsStreetAndZipButAccessorsExposeThem() {
        // given — a private-person invoice address, where street/zip are a home address
        InvoiceAddress address =
                new InvoiceAddress(STREET, CITY, ZIP_CODE, COUNTRY_CODE, null, null);

        // when
        String rendered = address.toString();

        // then — street and postal code are absent from toString...
        assertFalse(rendered.contains(STREET));
        assertFalse(rendered.contains(ZIP_CODE));
        // ...but still readable through the accessors
        assertEquals(STREET, address.street());
        assertEquals(ZIP_CODE, address.zipCode());
    }
}
