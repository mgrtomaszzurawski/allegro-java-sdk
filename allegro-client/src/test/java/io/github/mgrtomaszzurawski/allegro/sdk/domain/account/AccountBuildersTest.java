/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.CharitySearch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.ConversionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.ConversionStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Round-trip and validation tests for the charity and affiliate builders. */
class AccountBuildersTest {

    private static final String PHRASE = "children";
    private static final int CUSTOM_LIMIT = 20;
    private static final int TOO_LOW_LIMIT = 0;
    private static final int TOO_HIGH_LIMIT = CharitySearch.MAX_LIMIT + 1;
    private static final OffsetDateTime ORDER_FROM =
            OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORDER_TO =
            OffsetDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime MODIFIED_FROM =
            OffsetDateTime.of(2025, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime MODIFIED_TO =
            OffsetDateTime.of(2025, 1, 20, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String PARAM_KEY = "utm_source";

    @Test
    void charitySearch_whenPhraseAndLimitSet_buildsAndToBuilderPreserves() {
        // when
        CharitySearch search = CharitySearch.builder().phrase(PHRASE).limit(CUSTOM_LIMIT).build();

        // then
        assertEquals(PHRASE, search.phrase());
        assertEquals(CUSTOM_LIMIT, search.limit());
        CharitySearch copy = search.toBuilder().build();
        assertEquals(PHRASE, copy.phrase());
        assertEquals(CUSTOM_LIMIT, copy.limit());
    }

    @Test
    void charitySearch_whenLimitOmitted_defaultsToMax() {
        // when
        CharitySearch search = CharitySearch.builder().phrase(PHRASE).build();

        // then
        assertEquals(CharitySearch.MAX_LIMIT, search.limit());
    }

    @Test
    void charitySearch_whenPhraseMissing_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class, () -> CharitySearch.builder().build());
    }

    @Test
    void charitySearch_whenLimitBelowRange_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> CharitySearch.builder().phrase(PHRASE).limit(TOO_LOW_LIMIT).build());
    }

    @Test
    void charitySearch_whenLimitAboveRange_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> CharitySearch.builder().phrase(PHRASE).limit(TOO_HIGH_LIMIT).build());
    }

    @Test
    void conversionFilter_whenAll_hasNoCriteria() {
        // when
        ConversionFilter filter = ConversionFilter.all();

        // then
        assertNull(filter.orderCreatedFrom());
        assertNull(filter.orderCreatedTo());
        assertNull(filter.lastModifiedFrom());
        assertNull(filter.lastModifiedTo());
        assertNull(filter.status());
        assertTrue(filter.includePublisherUrlParameterKeys().isEmpty());
    }

    @Test
    void conversionFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        ConversionFilter filter = ConversionFilter.builder()
                .orderCreatedFrom(ORDER_FROM)
                .orderCreatedTo(ORDER_TO)
                .lastModifiedFrom(MODIFIED_FROM)
                .lastModifiedTo(MODIFIED_TO)
                .status(ConversionStatus.CONFIRMED)
                .includePublisherUrlParameterKeys(List.of(PARAM_KEY))
                .build();

        // then
        assertEquals(ORDER_FROM, filter.orderCreatedFrom());
        assertEquals(ORDER_TO, filter.orderCreatedTo());
        assertEquals(MODIFIED_FROM, filter.lastModifiedFrom());
        assertEquals(MODIFIED_TO, filter.lastModifiedTo());
        assertEquals(ConversionStatus.CONFIRMED, filter.status());
        assertEquals(List.of(PARAM_KEY), filter.includePublisherUrlParameterKeys());

        ConversionFilter copy = filter.toBuilder().build();
        assertEquals(ORDER_FROM, copy.orderCreatedFrom());
        assertEquals(ConversionStatus.CONFIRMED, copy.status());
        assertEquals(List.of(PARAM_KEY), copy.includePublisherUrlParameterKeys());
    }
}
