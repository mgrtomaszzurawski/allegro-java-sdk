/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesAddressRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import org.junit.jupiter.api.Test;

/** Construction-time validation and Raw mapping for {@link AfterSalesAddress}. */
class AfterSalesAddressTest {

    private static final String NAME = "Allegro sp. z o.o.";
    private static final String STREET = "Grunwaldzka 182";
    private static final String POST_CODE = "60-166";
    private static final String CITY = "Poznań";
    private static final String COUNTRY = "PL";

    @Test
    void constructor_whenAllFieldsSet_preservesValues() {
        AfterSalesAddress address = new AfterSalesAddress(NAME, STREET, POST_CODE, CITY, COUNTRY);

        assertEquals(NAME, address.name());
        assertEquals(STREET, address.street());
        assertEquals(POST_CODE, address.postCode());
        assertEquals(CITY, address.city());
        assertEquals(COUNTRY, address.countryCode());
    }

    @Test
    void constructor_whenCityBlank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new AfterSalesAddress(NAME, STREET, POST_CODE, "  ", COUNTRY));
    }

    @Test
    void constructor_whenCountryCodeNull_throws() {
        assertThrows(NullPointerException.class,
                () -> new AfterSalesAddress(NAME, STREET, POST_CODE, CITY, null));
    }

    @Test
    void from_whenRawGiven_mapsEveryField() {
        AfterSalesServicesAddressRaw raw = new AfterSalesServicesAddressRaw()
                .name(NAME)
                .street(STREET)
                .postCode(POST_CODE)
                .city(CITY)
                .countryCode(COUNTRY);

        AfterSalesAddress address = AfterSalesAddress.from(raw);

        // Assert every field — a transposition of the middle positional args
        // (street/postCode/city) in from() must not slip through.
        assertEquals(NAME, address.name());
        assertEquals(STREET, address.street());
        assertEquals(POST_CODE, address.postCode());
        assertEquals(CITY, address.city());
        assertEquals(COUNTRY, address.countryCode());
    }
}
