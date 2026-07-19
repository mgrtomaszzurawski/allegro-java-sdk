/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.LocationRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferLocation;
import org.junit.jupiter.api.Test;

class OfferLocationTest {

    private static final String CITY = "Warszawa";
    private static final String COUNTRY_CODE = "PL";
    private static final String POST_CODE = "00-001";
    private static final String PROVINCE = "mazowieckie";

    @Test
    void build_whenAllFieldsSet_exposesEachValue() {
        OfferLocation location = OfferLocation.builder()
                .city(CITY).countryCode(COUNTRY_CODE).postCode(POST_CODE).province(PROVINCE).build();

        assertEquals(CITY, location.city());
        assertEquals(COUNTRY_CODE, location.countryCode());
        assertEquals(POST_CODE, location.postCode());
        assertEquals(PROVINCE, location.province());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        OfferLocation original = OfferLocation.builder()
                .city(CITY).countryCode(COUNTRY_CODE).postCode(POST_CODE).province(PROVINCE).build();

        assertEquals(original, original.toBuilder().build());
    }

    @Test
    void from_whenResponsePresent_mapsEveryField() {
        LocationRaw raw = new LocationRaw()
                .city(CITY).countryCode(COUNTRY_CODE).postCode(POST_CODE).province(PROVINCE);

        OfferLocation location = OfferLocation.from(raw);

        assertEquals(new OfferLocation(CITY, COUNTRY_CODE, POST_CODE, PROVINCE), location);
    }

    @Test
    void from_whenNull_returnsNull() {
        assertNull(OfferLocation.from(null));
    }

    @Test
    void toRaw_whenMapped_carriesEveryField() {
        LocationRaw raw = OfferLocation.builder()
                .city(CITY).countryCode(COUNTRY_CODE).postCode(POST_CODE).province(PROVINCE).build().toRaw();

        assertEquals(CITY, raw.getCity());
        assertEquals(COUNTRY_CODE, raw.getCountryCode());
        assertEquals(POST_CODE, raw.getPostCode());
        assertEquals(PROVINCE, raw.getProvince());
    }
}
