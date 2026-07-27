/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ClassifiedsPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreviewSellingMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ParameterRange;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation for {@link OfferFeePreviewRequest#builder()}:
 * the required category and selling mode, the optional fee-affecting inputs, and
 * {@code toBuilder()} preservation.
 */
class OfferFeePreviewRequestBuilderTest {

    private static final String TEST_CATEGORY_ID = "257";
    private static final String PRICE_AMOUNT = "99.99";
    private static final String NET_AMOUNT = "81.29";
    private static final String STARTING_AMOUNT = "10.00";
    private static final String MINIMAL_AMOUNT = "50.00";
    private static final String TEST_CURRENCY = "PLN";
    private static final String TEST_OFFER_ID = "654321";
    private static final String TEST_MARKETPLACE = "allegro-pl";
    private static final String TEST_DURATION = "P30D";
    private static final String CAMPAIGN_ID = "camp-1";
    private static final String PARAM_ID = "11323";
    private static final String PARAM_VALUE = "Red";
    private static final String CATEGORY_TOKEN = "categoryId";
    private static final String SELLING_MODE_TOKEN = "selling mode";

    private static Money price() {
        return Money.of(PRICE_AMOUNT, TEST_CURRENCY);
    }

    @Test
    void build_whenRequiredFieldsOnly_buildsBuyNowRequestWithDefaults() {
        // when
        OfferFeePreviewRequest request = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .price(price())
                .build();

        // then — the price shortcut yields a Buy Now selling mode, optionals empty
        assertEquals(TEST_CATEGORY_ID, request.categoryId());
        assertEquals(FeePreviewSellingMode.buyNow(price()), request.sellingMode());
        assertEquals(price(), request.buyNowPrice());
        assertNull(request.offerId());
        assertNull(request.marketplaceId());
        assertTrue(request.parameters().isEmpty());
        assertNull(request.classifiedsPackages());
    }

    @Test
    void build_whenAllFeeAffectingInputsSet_carriesThemAll() {
        // when
        OfferFeePreviewRequest request = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .sellingMode(FeePreviewSellingMode.buyNow(price(), Money.of(NET_AMOUNT, TEST_CURRENCY)))
                .offerId(TEST_OFFER_ID)
                .marketplaceId(TEST_MARKETPLACE)
                .fundraisingCampaignId(CAMPAIGN_ID)
                .publicationDuration(TEST_DURATION)
                .emphasizedForTenDays()
                .onDepartmentPage()
                .addParameter(OfferParameter.ofValues(PARAM_ID, List.of(PARAM_VALUE)))
                .classifiedsPackages(ClassifiedsPackages.ofBasePackage("base-1"))
                .build();

        // then
        assertEquals(TEST_OFFER_ID, request.offerId());
        assertEquals(TEST_MARKETPLACE, request.marketplaceId());
        assertEquals(CAMPAIGN_ID, request.fundraisingCampaignId());
        assertEquals(TEST_DURATION, request.publicationDuration());
        assertTrue(request.promotionOptions().emphasized10d());
        assertTrue(request.promotionOptions().departmentPage());
        assertEquals(1, request.parameters().size());
        assertEquals("base-1", request.classifiedsPackages().basePackageId());
    }

    @Test
    void build_whenAuctionSellingMode_carriesStartingAndMinimalPrice() {
        // when
        OfferFeePreviewRequest request = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .sellingMode(FeePreviewSellingMode.auction(
                        Money.of(STARTING_AMOUNT, TEST_CURRENCY), Money.of(MINIMAL_AMOUNT, TEST_CURRENCY)))
                .build();

        // then
        FeePreviewSellingMode.Auction auction =
                assertInstanceOf(FeePreviewSellingMode.Auction.class, request.sellingMode());
        assertEquals(Money.of(STARTING_AMOUNT, TEST_CURRENCY), auction.startingPrice());
        assertEquals(Money.of(MINIMAL_AMOUNT, TEST_CURRENCY), auction.minimalPrice());
        assertNull(request.buyNowPrice());
    }

    @Test
    void build_whenParameterIsRange_preservesRange() {
        // when
        OfferFeePreviewRequest request = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .price(price())
                .addParameter(OfferParameter.ofRange(PARAM_ID, new ParameterRange("1", "5")))
                .build();

        // then
        ParameterRange range = request.parameters().get(0).rangeValue();
        assertEquals("1", range.lowerBound());
        assertEquals("5", range.upperBound());
    }

    @Test
    void toBuilder_preservesAllFields() {
        // given
        OfferFeePreviewRequest original = OfferFeePreviewRequest.builder()
                .categoryId(TEST_CATEGORY_ID)
                .sellingMode(FeePreviewSellingMode.buyNow(price(), Money.of(NET_AMOUNT, TEST_CURRENCY)))
                .offerId(TEST_OFFER_ID)
                .marketplaceId(TEST_MARKETPLACE)
                .publicationDuration(TEST_DURATION)
                .emphasizedForOneDay()
                .addParameter(OfferParameter.ofValueIds(PARAM_ID, List.of("1")))
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
    void build_whenSellingModeMissing_throwsIllegalState() {
        // given — neither price() nor sellingMode() set
        var builder = OfferFeePreviewRequest.builder().categoryId(TEST_CATEGORY_ID);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(SELLING_MODE_TOKEN));
    }
}
