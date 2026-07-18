/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation for {@link OfferFeePreviewRequest#builder()}:
 * the required category id and price, the optional offer id, and
 * {@code toBuilder()} preservation.
 */
class OfferFeePreviewRequestBuilderTest {

    private static final String TEST_CATEGORY_ID = "257";
    private static final String PRICE_AMOUNT = "99.99";
    private static final String TEST_CURRENCY = "PLN";
    private static final String TEST_OFFER_ID = "654321";
    private static final String CATEGORY_TOKEN = "categoryId";
    private static final String PRICE_TOKEN = "price";

    private static Money price() {
        return Money.of(PRICE_AMOUNT, TEST_CURRENCY);
    }

    @Test
    void build_whenRequiredFieldsOnly_buildsRequestWithoutOfferId() {
        // when
        OfferFeePreviewRequest request = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .price(price())
                .build();

        // then
        assertEquals(TEST_CATEGORY_ID, request.categoryId());
        assertEquals(price(), request.price());
        assertNull(request.offerId());
    }

    @Test
    void build_whenOfferIdSet_includesOfferId() {
        // when
        OfferFeePreviewRequest request = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .price(price())
                .offerId(TEST_OFFER_ID)
                .build();

        // then
        assertEquals(TEST_OFFER_ID, request.offerId());
    }

    @Test
    void toBuilder_preservesAllFields() {
        // given
        OfferFeePreviewRequest original = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .price(price())
                .offerId(TEST_OFFER_ID)
                .build();

        // when
        OfferFeePreviewRequest rebuilt = original.toBuilder().build();

        // then
        assertEquals(original, rebuilt);
    }

    @Test
    void build_whenCategoryMissing_throwsIllegalState() {
        // given
        var builder = OfferFeePreviewRequest.builder().price(price());

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(CATEGORY_TOKEN));
    }

    @Test
    void build_whenPriceMissing_throwsIllegalState() {
        // given
        var builder = OfferFeePreviewRequest.builder().categoryId(TEST_CATEGORY_ID);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(PRICE_TOKEN));
    }
}
