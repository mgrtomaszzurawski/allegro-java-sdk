/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Benefit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferCriterion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast tests for {@link PromotionRequest#builder()}: a
 * promotion needs at least one benefit and at least one offer criterion.
 */
class PromotionRequestBuilderTest {

    private static final String TEST_AMOUNT = "100.00";
    private static final String TEST_CURRENCY = "PLN";
    private static final String TEST_PERCENTAGE = "10";
    private static final String MULTIPACK_PERCENTAGE = "50";
    private static final String TEST_OFFER_ID = "12345";

    private static final String ERR_NO_BENEFIT = "at least one benefit is required";
    private static final String ERR_NO_CRITERION = "at least one offer criterion is required";

    private static Benefit largeOrderBenefit() {
        return new Benefit.LargeOrderDiscount(List.of(new Benefit.OrderValueThreshold(
                Money.of(TEST_AMOUNT, TEST_CURRENCY), TEST_PERCENTAGE)));
    }

    private static Benefit multiPackBenefit() {
        return new Benefit.MultiPackDiscount(MULTIPACK_PERCENTAGE, new BigDecimal(3), new BigDecimal(1));
    }

    @Test
    void build_whenRequiredFieldsOnly_buildsRequest() {
        // given / when
        PromotionRequest request = PromotionRequest.builder()
                .addBenefit(largeOrderBenefit())
                .addOfferCriterion(OfferCriterion.allOffers())
                .build();

        // then
        assertEquals(1, request.benefits().size());
        assertEquals(1, request.offerCriteria().size());
        assertEquals(OfferCriterion.Type.ALL_OFFERS, request.offerCriteria().get(0).type());
    }

    @Test
    void build_whenAllCoreFieldsSet_buildsRequest() {
        // given / when — the list setters replace any accumulated items
        PromotionRequest request = PromotionRequest.builder()
                .benefits(List.of(largeOrderBenefit(), multiPackBenefit()))
                .offerCriteria(List.of(OfferCriterion.containing(List.of(TEST_OFFER_ID))))
                .build();

        // then
        assertEquals(2, request.benefits().size());
        assertEquals(List.of(TEST_OFFER_ID), request.offerCriteria().get(0).offerIds());
    }

    @Test
    void toBuilder_preservesBenefitsAndCriteria() {
        // given
        PromotionRequest original = PromotionRequest.builder()
                .addBenefit(largeOrderBenefit())
                .addOfferCriterion(OfferCriterion.containing(List.of(TEST_OFFER_ID)))
                .build();

        // when
        PromotionRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenNoBenefit_throws() {
        // given
        var builder = PromotionRequest.builder().addOfferCriterion(OfferCriterion.allOffers());

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_NO_BENEFIT, failure.getMessage());
    }

    @Test
    void build_whenNoCriterion_throws() {
        // given
        var builder = PromotionRequest.builder().addBenefit(largeOrderBenefit());

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_NO_CRITERION, failure.getMessage());
    }
}
