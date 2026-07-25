/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxRateRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxRate;
import org.junit.jupiter.api.Test;

class TaxRateTest {

    private static final String RATE = "5.00";
    private static final String COUNTRY_CODE = "PL";

    @Test
    void toRaw_whenBothFieldsSet_writesThem() {
        // when
        OfferTaxRateRaw raw = TaxRate.of(RATE, COUNTRY_CODE).toRaw();

        // then
        assertEquals(RATE, raw.getRate());
        assertEquals(COUNTRY_CODE, raw.getCountryCode());
    }

    @Test
    void toRaw_whenFieldsUnset_omitsThem() {
        // given — a rate with neither field set
        TaxRate rate = new TaxRate(null, null);

        // when
        OfferTaxRateRaw raw = rate.toRaw();

        // then — unset fields are left off the body
        assertNull(raw.getRate());
        assertNull(raw.getCountryCode());
    }

    @Test
    void from_whenRawNull_returnsNull() {
        // then
        assertNull(TaxRate.from(null));
    }

    @Test
    void from_whenRawPresent_mapsBothFields() {
        // given
        OfferTaxRateRaw raw = new OfferTaxRateRaw().rate(RATE).countryCode(COUNTRY_CODE);

        // when
        TaxRate rate = TaxRate.from(raw);

        // then
        assertEquals(RATE, rate.rate());
        assertEquals(COUNTRY_CODE, rate.countryCode());
    }
}
