/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange.CurrencyBasis;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.RuleAssignment;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Fail-fast validation and accessors of the {@link BatchPricingRulesRequest}
 * builder. One failure test per required field (TESTING.md §1); the wire mapping
 * is covered separately by {@code PricingRulesMapperTest}.
 */
class BatchPricingRulesRequestTest {

    private static final String OFFER_ID = "111";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String RULE_ID = "641c73feaef0a8281a3d11f8";
    private static final String AMOUNT = "10.00";
    private static final String CURRENCY_PLN = "PLN";

    @Test
    void assignRules_whenNullOffers_throws() {
        // given/when/then — a null offer list is rejected
        assertThrows(NullPointerException.class, () -> BatchPricingRulesRequest.assignRules(null));
    }

    @Test
    void assignRules_whenEmptyOffers_throws() {
        // given/when/then — an empty offer list is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchPricingRulesRequest.assignRules(List.of()));
    }

    @Test
    void assignRules_whenBlankOfferId_throws() {
        // given/when/then — a blank offer id is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchPricingRulesRequest.assignRules(List.of(" ")));
    }

    @Test
    void assignRules_whenTooManyOffers_throws() {
        // given — one more than the per-command maximum
        List<String> tooMany = IntStream.rangeClosed(0, BatchPricingRulesRequest.MAX_OFFERS)
                .mapToObj(Integer::toString).toList();

        // when/then — the over-limit list is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchPricingRulesRequest.assignRules(tooMany));
    }

    @Test
    void onMarketplace_whenBlankMarketplace_throws() {
        // given/when/then — a blank marketplace id is rejected
        BatchPricingRulesRequest.AssignBuilder builder =
                BatchPricingRulesRequest.assignRules(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.onMarketplace(" ", RULE_ID));
    }

    @Test
    void onMarketplace_whenBlankRuleId_throws() {
        // given/when/then — a blank rule id is rejected
        BatchPricingRulesRequest.AssignBuilder builder =
                BatchPricingRulesRequest.assignRules(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.onMarketplace(MARKETPLACE_PL, " "));
    }

    @Test
    void assignBuild_whenNoMarketplace_throws() {
        // given — no assignment added
        BatchPricingRulesRequest.AssignBuilder builder =
                BatchPricingRulesRequest.assignRules(List.of(OFFER_ID));

        // when/then — build fails fast
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void fromMarketplace_whenBlankMarketplace_throws() {
        // given/when/then — a blank marketplace id is rejected
        BatchPricingRulesRequest.RemoveBuilder builder =
                BatchPricingRulesRequest.removeRules(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.fromMarketplace(" "));
    }

    @Test
    void removeBuild_whenNoMarketplace_throws() {
        // given — no removal added
        BatchPricingRulesRequest.RemoveBuilder builder =
                BatchPricingRulesRequest.removeRules(List.of(OFFER_ID));

        // when/then — build fails fast
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void priceRangeOf_whenNullBasis_throws() {
        // given/when/then — a null currency basis is rejected
        assertThrows(NullPointerException.class, () -> PriceRange.of(
                null, Money.of(AMOUNT, CURRENCY_PLN), Money.of(AMOUNT, CURRENCY_PLN)));
    }

    @Test
    void priceRangeOf_whenNullMinPrice_throws() {
        // given/when/then — a null minimum price is rejected
        assertThrows(NullPointerException.class, () -> PriceRange.of(
                CurrencyBasis.MARKETPLACE_CURRENCY, null, Money.of(AMOUNT, CURRENCY_PLN)));
    }

    @Test
    void priceRangeOf_whenNullMaxPrice_throws() {
        // given/when/then — a null maximum price is rejected
        assertThrows(NullPointerException.class, () -> PriceRange.of(
                CurrencyBasis.MARKETPLACE_CURRENCY, Money.of(AMOUNT, CURRENCY_PLN), null));
    }

    @Test
    void assignRules_whenBuilt_exposesAssignmentIntent() {
        // given — an assignment with a price range on one marketplace
        PriceRange range = PriceRange.of(CurrencyBasis.MARKETPLACE_CURRENCY,
                Money.of(AMOUNT, CURRENCY_PLN), Money.of(AMOUNT, CURRENCY_PLN));
        BatchPricingRulesRequest request = BatchPricingRulesRequest.assignRules(List.of(OFFER_ID))
                .onMarketplace(MARKETPLACE_PL, RULE_ID, range)
                .build();

        // then — the request reads back as an assignment carrying the built data
        assertTrue(request.isAssignment());
        assertEquals(List.of(OFFER_ID), request.offerIds());
        assertTrue(request.removalMarketplaceIds().isEmpty());
        assertEquals(1, request.assignments().size());
        RuleAssignment assignment = request.assignments().get(0);
        assertEquals(MARKETPLACE_PL, assignment.marketplaceId());
        assertEquals(RULE_ID, assignment.ruleId());
        assertEquals(range, assignment.configuration());
    }

    @Test
    void onMarketplace_whenNoConfiguration_leavesConfigurationNull() {
        // given — a config-less assignment
        BatchPricingRulesRequest request = BatchPricingRulesRequest.assignRules(List.of(OFFER_ID))
                .onMarketplace(MARKETPLACE_PL, RULE_ID)
                .build();

        // then — the assignment carries no configuration
        assertNull(request.assignments().get(0).configuration());
    }

    @Test
    void removeRules_whenBuilt_exposesRemovalIntent() {
        // given — a removal on one marketplace
        BatchPricingRulesRequest request = BatchPricingRulesRequest.removeRules(List.of(OFFER_ID))
                .fromMarketplace(MARKETPLACE_PL)
                .build();

        // then — the request reads back as a removal carrying the built data
        assertFalse(request.isAssignment());
        assertTrue(request.assignments().isEmpty());
        assertEquals(List.of(MARKETPLACE_PL), request.removalMarketplaceIds());
    }

    @Test
    void offerIds_whenReadFromRequest_isImmutable() {
        // given — a built request
        BatchPricingRulesRequest request = BatchPricingRulesRequest.removeRules(List.of(OFFER_ID))
                .fromMarketplace(MARKETPLACE_PL)
                .build();

        // then — the exposed offer ids cannot be mutated by the caller
        List<String> offerIds = request.offerIds();
        assertThrows(UnsupportedOperationException.class, () -> offerIds.add("999"));
    }
}
