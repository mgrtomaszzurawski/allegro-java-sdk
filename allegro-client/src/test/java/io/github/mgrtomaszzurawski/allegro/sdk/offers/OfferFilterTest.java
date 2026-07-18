/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import org.junit.jupiter.api.Test;

class OfferFilterTest {

    private static final String NAME = "keyboard";
    private static final String PRICE_FROM = "10.00";
    private static final String PRICE_TO = "500.00";

    @Test
    void all_whenNoFilters_leavesEveryFieldUnset() {
        // when
        OfferFilter filter = OfferFilter.all();

        // then — an empty filter carries nothing (every field omitted from the query)
        assertNull(filter.name());
        assertNull(filter.status());
        assertNull(filter.format());
        assertNull(filter.priceFrom());
        assertNull(filter.priceTo());
    }

    @Test
    void builder_whenAllFieldsSet_exposesEachValue() {
        // when
        OfferFilter filter = OfferFilter.builder()
                .name(NAME)
                .status(OfferStatus.ACTIVE)
                .format(OfferFormat.BUY_NOW)
                .priceFrom(PRICE_FROM)
                .priceTo(PRICE_TO)
                .build();

        // then
        assertEquals(NAME, filter.name());
        assertEquals(OfferStatus.ACTIVE, filter.status());
        assertEquals(OfferFormat.BUY_NOW, filter.format());
        assertEquals(PRICE_FROM, filter.priceFrom());
        assertEquals(PRICE_TO, filter.priceTo());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        // given
        OfferFilter original = OfferFilter.builder()
                .name(NAME)
                .status(OfferStatus.INACTIVE)
                .format(OfferFormat.AUCTION)
                .priceFrom(PRICE_FROM)
                .priceTo(PRICE_TO)
                .build();

        // when
        OfferFilter copy = original.toBuilder().build();

        // then — a round-trip through the builder changes nothing
        assertEquals(original.name(), copy.name());
        assertEquals(original.status(), copy.status());
        assertEquals(original.format(), copy.format());
        assertEquals(original.priceFrom(), copy.priceFrom());
        assertEquals(original.priceTo(), copy.priceTo());
    }
}
