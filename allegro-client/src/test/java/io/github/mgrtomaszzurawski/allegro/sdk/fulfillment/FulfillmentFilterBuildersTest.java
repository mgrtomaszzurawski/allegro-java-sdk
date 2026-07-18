/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RefundDispositionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.StockFilter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Round-trip proof for the fulfillment filter builders: every field set is read
 * back, {@code toBuilder()} preserves it, and {@code all()} leaves everything
 * unset. Satisfies the per-builder METHOD=1.00 gate.
 */
class FulfillmentFilterBuildersTest {

    private static final String TEST_PHRASE = "headphones";
    private static final String TEST_SORT = "-outOfStockIn";
    private static final String TEST_PRODUCT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String TEST_PRODUCT_AVAILABILITY = "AVAILABLE";
    private static final String TEST_PRODUCT_STATUS = "ACTIVE";
    private static final String TEST_ASN_STATUS = "RECEIVED";
    private static final int TEST_OUT_OF_STOCK_FROM = 3;
    private static final int TEST_OUT_OF_STOCK_TO = 30;

    private static final OffsetDateTime CREATED_FROM =
            OffsetDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime CREATED_TO =
            OffsetDateTime.of(2026, 7, 31, 23, 59, 0, 0, ZoneOffset.UTC);

    private static StockFilter fullStockFilter() {
        return StockFilter.builder()
                .phrase(TEST_PHRASE)
                .sort(TEST_SORT)
                .productId(TEST_PRODUCT_ID)
                .productAvailability(TEST_PRODUCT_AVAILABILITY)
                .productStatus(TEST_PRODUCT_STATUS)
                .asnStatus(TEST_ASN_STATUS)
                .outOfStockInFrom(TEST_OUT_OF_STOCK_FROM)
                .outOfStockInTo(TEST_OUT_OF_STOCK_TO)
                .build();
    }

    private static void assertFullStockFilter(StockFilter filter) {
        assertEquals(TEST_PHRASE, filter.phrase());
        assertEquals(TEST_SORT, filter.sort());
        assertEquals(TEST_PRODUCT_ID, filter.productId());
        assertEquals(TEST_PRODUCT_AVAILABILITY, filter.productAvailability());
        assertEquals(TEST_PRODUCT_STATUS, filter.productStatus());
        assertEquals(TEST_ASN_STATUS, filter.asnStatus());
        assertEquals(TEST_OUT_OF_STOCK_FROM, filter.outOfStockInFrom());
        assertEquals(TEST_OUT_OF_STOCK_TO, filter.outOfStockInTo());
    }

    @Test
    void stockFilter_whenAllFieldsSet_readsThemBack() {
        // given / when
        StockFilter filter = fullStockFilter();

        // then
        assertFullStockFilter(filter);
    }

    @Test
    void stockFilter_toBuilder_preservesAllFields() {
        // given
        StockFilter original = fullStockFilter();

        // when
        StockFilter copy = original.toBuilder().build();

        // then
        assertFullStockFilter(copy);
    }

    @Test
    void stockFilter_all_leavesEveryFieldUnset() {
        // when
        StockFilter filter = StockFilter.all();

        // then
        assertNull(filter.phrase());
        assertNull(filter.sort());
        assertNull(filter.productId());
        assertNull(filter.productAvailability());
        assertNull(filter.productStatus());
        assertNull(filter.asnStatus());
        assertNull(filter.outOfStockInFrom());
        assertNull(filter.outOfStockInTo());
    }

    @Test
    void refundDispositionFilter_whenBoundsSet_readsThemBack() {
        // given / when
        RefundDispositionFilter filter = RefundDispositionFilter.builder()
                .createdFrom(CREATED_FROM)
                .createdTo(CREATED_TO)
                .build();

        // then
        assertEquals(CREATED_FROM, filter.createdFrom());
        assertEquals(CREATED_TO, filter.createdTo());
    }

    @Test
    void refundDispositionFilter_toBuilder_preservesBounds() {
        // given
        RefundDispositionFilter original = RefundDispositionFilter.builder()
                .createdFrom(CREATED_FROM)
                .createdTo(CREATED_TO)
                .build();

        // when
        RefundDispositionFilter copy = original.toBuilder().build();

        // then
        assertEquals(CREATED_FROM, copy.createdFrom());
        assertEquals(CREATED_TO, copy.createdTo());
    }

    @Test
    void refundDispositionFilter_all_leavesBoundsUnset() {
        // when
        RefundDispositionFilter filter = RefundDispositionFilter.all();

        // then
        assertNull(filter.createdFrom());
        assertNull(filter.createdTo());
    }
}
