/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxSettings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxSettingsTest {

    private static final String RATE = "23";
    private static final String COUNTRY_CODE = "PL";
    private static final String SUBJECT = "GOODS";
    private static final String EXEMPTION = "NONE";

    @Test
    void build_whenAllFieldsSet_exposesEachValue() {
        // when
        TaxSettings settings = TaxSettings.builder()
                .subject(SUBJECT)
                .exemption(EXEMPTION)
                .rates(List.of(TaxRate.of(RATE, COUNTRY_CODE)))
                .build();

        // then
        assertEquals(SUBJECT, settings.subject());
        assertEquals(EXEMPTION, settings.exemption());
        assertEquals(1, settings.rates().size());
        assertEquals(RATE, settings.rates().get(0).rate());
        assertEquals(COUNTRY_CODE, settings.rates().get(0).countryCode());
    }

    @Test
    void build_whenNoRates_defaultsToEmptyList() {
        // when
        TaxSettings settings = TaxSettings.builder().build();

        // then
        assertNull(settings.subject());
        assertNull(settings.exemption());
        assertTrue(settings.rates().isEmpty());
    }

    @Test
    void toRaw_whenBuilt_writesEveryField() {
        // given
        TaxSettings settings = TaxSettings.builder()
                .subject(SUBJECT).exemption(EXEMPTION)
                .rates(List.of(TaxRate.of(RATE, COUNTRY_CODE))).build();

        // when
        OfferTaxSettingsRaw raw = settings.toRaw();

        // then
        assertEquals(SUBJECT, raw.getSubject());
        assertEquals(EXEMPTION, raw.getExemption());
        assertEquals(1, raw.getRates().size());
        assertEquals(RATE, raw.getRates().get(0).getRate());
        assertEquals(COUNTRY_CODE, raw.getRates().get(0).getCountryCode());
    }

    @Test
    void from_whenRawPresent_mapsSubjectExemptionAndRates() {
        // given
        OfferTaxSettingsRaw raw = new OfferTaxSettingsRaw()
                .subject(SUBJECT)
                .exemption(EXEMPTION)
                .rates(List.of(new OfferTaxRateRaw().rate(RATE).countryCode(COUNTRY_CODE)));

        // when
        TaxSettings settings = TaxSettings.from(raw);

        // then
        assertEquals(SUBJECT, settings.subject());
        assertEquals(EXEMPTION, settings.exemption());
        assertEquals(1, settings.rates().size());
        assertEquals(RATE, settings.rates().get(0).rate());
        assertEquals(COUNTRY_CODE, settings.rates().get(0).countryCode());
    }

    @Test
    void from_whenRawNull_returnsNull() {
        // then
        assertNull(TaxSettings.from(null));
    }

    @Test
    void toRaw_whenRatesEmpty_leavesRatesEmpty() {
        // given — only scalars set, no rates
        TaxSettings settings = TaxSettings.builder().subject(SUBJECT).build();

        // when
        OfferTaxSettingsRaw raw = settings.toRaw();

        // then — the empty-rates branch leaves the generated default (an empty list): the
        // mapper never populates a rate, so nothing is written for it
        assertTrue(raw.getRates().isEmpty());
    }

    @Test
    void toRaw_whenScalarsUnset_omitsThem() {
        // given — only a rate set, no subject/exemption
        TaxSettings settings = TaxSettings.builder()
                .rates(List.of(TaxRate.of(RATE, COUNTRY_CODE))).build();

        // when
        OfferTaxSettingsRaw raw = settings.toRaw();

        // then — unset scalars are left off the body
        assertNull(raw.getSubject());
        assertNull(raw.getExemption());
    }

    @Test
    void from_whenRateElementNull_filtersItOut() {
        // given — a payload whose rates list carries a null element (defensive against a
        // sparse array); the null must be dropped before the immutable copy
        OfferTaxSettingsRaw raw = new OfferTaxSettingsRaw()
                .rates(Arrays.asList(new OfferTaxRateRaw().rate(RATE).countryCode(COUNTRY_CODE), null));

        // when
        TaxSettings settings = TaxSettings.from(raw);

        // then — only the non-null rate survives
        assertEquals(1, settings.rates().size());
        assertEquals(RATE, settings.rates().get(0).rate());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        // given
        TaxSettings original = TaxSettings.builder()
                .subject(SUBJECT).exemption(EXEMPTION)
                .rates(List.of(TaxRate.of(RATE, COUNTRY_CODE))).build();

        // when
        TaxSettings copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }
}
