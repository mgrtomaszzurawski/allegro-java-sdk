/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerAddressRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import org.junit.jupiter.api.Test;

/** Raw mapping for {@link ResponsiblePartyAddress} (person enum vs producer string country). */
class ResponsiblePartyAddressTest {

    private static final String COUNTRY = "PL";
    private static final String STREET = "Wiśniowa 1";
    private static final String POSTAL_CODE = "00-000";
    private static final String CITY = "Warszawa";

    @Test
    void from_whenPersonAddressRaw_mapsEnumCountryToString() {
        ResponsiblePersonAddressRaw raw = new ResponsiblePersonAddressRaw()
                .countryCode(ResponsiblePersonAddressRaw.CountryCodeEnum.PL)
                .street(STREET).postalCode(POSTAL_CODE).city(CITY);

        ResponsiblePartyAddress address = ResponsiblePartyAddress.from(raw);

        assertEquals(COUNTRY, address.countryCode());
        assertEquals(STREET, address.street());
        assertEquals(POSTAL_CODE, address.postalCode());
        assertEquals(CITY, address.city());
    }

    @Test
    void from_whenProducerAddressRaw_mapsStringCountry() {
        ResponsibleProducerAddressRaw raw = new ResponsibleProducerAddressRaw()
                .countryCode(COUNTRY).street(STREET).postalCode(POSTAL_CODE).city(CITY);

        ResponsiblePartyAddress address = ResponsiblePartyAddress.from(raw);

        assertEquals(COUNTRY, address.countryCode());
        assertEquals(CITY, address.city());
    }

    @Test
    void from_whenPersonAddressCountryAbsent_mapsCountryToNull() {
        ResponsiblePersonAddressRaw raw = new ResponsiblePersonAddressRaw().street(STREET).city(CITY);

        ResponsiblePartyAddress address = ResponsiblePartyAddress.from(raw);

        assertNull(address.countryCode());
        assertEquals(STREET, address.street());
    }

    @Test
    void from_whenNullProducerRaw_returnsNull() {
        assertNull(ResponsiblePartyAddress.from((ResponsibleProducerAddressRaw) null));
    }
}
