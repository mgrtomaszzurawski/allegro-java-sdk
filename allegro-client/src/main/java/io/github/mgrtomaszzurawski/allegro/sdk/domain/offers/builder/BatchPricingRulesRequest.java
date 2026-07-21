/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A batch automatic-pricing-rules change — the request passed to
 * {@code offers().batch().applyPricingRules(...)}. One command either
 * <em>assigns</em> a pricing rule to the target offers on one or more
 * marketplaces ({@link #assignRules}) or <em>removes</em> the rules from the
 * target offers on one or more marketplaces ({@link #removeRules}); the two are
 * mutually exclusive (the wire models the modification as a {@code oneOf}), so
 * each entry point produces its own builder.
 *
 * <p>The offers are given once (up to {@value #MAX_OFFERS}); every marketplace
 * change in the request applies to all of them. An assignment may carry an
 * optional {@link PriceRange} configuration bounding the price the rule may set.
 * The mapping to the wire is the SDK's job; this type only carries intent.
 *
 * @since 0.5.0
 */
public final class BatchPricingRulesRequest {

    /** Allegro accepts up to 1000 offers in one criterion. */
    public static final int MAX_OFFERS = 1000;

    private static final String ERR_OFFERS_EMPTY = "at least one offer id is required";
    private static final String ERR_OFFERS_TOO_MANY = "at most " + MAX_OFFERS + " offers per command";
    private static final String ERR_OFFER_ID = "offer id must not be null or blank";
    private static final String ERR_MARKETPLACE = "marketplace must not be null or blank";
    private static final String ERR_RULE_ID = "rule id must not be null or blank";
    private static final String ERR_NO_ASSIGNMENT = "at least one marketplace assignment is required";
    private static final String ERR_NO_REMOVAL = "at least one marketplace removal is required";

    private final List<String> offerIds;
    // Exactly one of these is populated (spec oneOf: set XOR remove).
    private final List<RuleAssignment> assignments;
    private final List<String> removalMarketplaceIds;

    private BatchPricingRulesRequest(List<String> offerIds, List<RuleAssignment> assignments,
            List<String> removalMarketplaceIds) {
        this.offerIds = List.copyOf(offerIds);
        this.assignments = List.copyOf(assignments);
        this.removalMarketplaceIds = List.copyOf(removalMarketplaceIds);
    }

    /**
     * Start an assignment command targeting {@code offerIds}: rules will be added
     * to these offers on the marketplaces given to the returned builder.
     */
    public static AssignBuilder assignRules(List<String> offerIds) {
        return new AssignBuilder(offerIds);
    }

    /**
     * Start a removal command targeting {@code offerIds}: rules will be removed
     * from these offers on the marketplaces given to the returned builder.
     */
    public static RemoveBuilder removeRules(List<String> offerIds) {
        return new RemoveBuilder(offerIds);
    }

    /** The offers this command targets. */
    public List<String> offerIds() {
        return offerIds;
    }

    /** {@code true} for an assignment command, {@code false} for a removal command. */
    public boolean isAssignment() {
        return removalMarketplaceIds.isEmpty();
    }

    /** The per-marketplace rule assignments; empty for a removal command. */
    public List<RuleAssignment> assignments() {
        return assignments;
    }

    /** The marketplace ids to remove rules from; empty for an assignment command. */
    public List<String> removalMarketplaceIds() {
        return removalMarketplaceIds;
    }

    private static List<String> validatedOfferIds(List<String> offerIds) {
        Objects.requireNonNull(offerIds, ERR_OFFERS_EMPTY);
        if (offerIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_OFFERS_EMPTY);
        }
        if (offerIds.size() > MAX_OFFERS) {
            throw new IllegalArgumentException(ERR_OFFERS_TOO_MANY);
        }
        for (String offerId : offerIds) {
            requireText(offerId, ERR_OFFER_ID);
        }
        return offerIds;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /** Fluent builder for an assignment command; validates fail-fast on {@link #build()}. */
    public static final class AssignBuilder {

        private final List<String> offerIds;
        private final List<RuleAssignment> assignments = new ArrayList<>();

        private AssignBuilder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Assign {@code ruleId} on {@code marketplace} (e.g. {@code "allegro-pl"}). */
        public AssignBuilder onMarketplace(String marketplace, String ruleId) {
            return onMarketplace(marketplace, ruleId, null);
        }

        /**
         * Assign {@code ruleId} on {@code marketplace}, bounding the price the rule
         * may set to {@code configuration}'s range.
         */
        public AssignBuilder onMarketplace(String marketplace, String ruleId,
                @Nullable PriceRange configuration) {
            assignments.add(new RuleAssignment(requireText(marketplace, ERR_MARKETPLACE),
                    requireText(ruleId, ERR_RULE_ID), configuration));
            return this;
        }

        /** Build, requiring at least one marketplace assignment. */
        public BatchPricingRulesRequest build() {
            if (assignments.isEmpty()) {
                throw new IllegalStateException(ERR_NO_ASSIGNMENT);
            }
            return new BatchPricingRulesRequest(offerIds, assignments, List.of());
        }
    }

    /** Fluent builder for a removal command; validates fail-fast on {@link #build()}. */
    public static final class RemoveBuilder {

        private final List<String> offerIds;
        private final List<String> marketplaceIds = new ArrayList<>();

        private RemoveBuilder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Remove the assigned rules on {@code marketplace} (e.g. {@code "allegro-pl"}). */
        public RemoveBuilder fromMarketplace(String marketplace) {
            marketplaceIds.add(requireText(marketplace, ERR_MARKETPLACE));
            return this;
        }

        /** Build, requiring at least one marketplace removal. */
        public BatchPricingRulesRequest build() {
            if (marketplaceIds.isEmpty()) {
                throw new IllegalStateException(ERR_NO_REMOVAL);
            }
            return new BatchPricingRulesRequest(offerIds, List.of(), marketplaceIds);
        }
    }

    /**
     * One marketplace's rule assignment: the marketplace, the rule to apply, and
     * an optional {@link PriceRange} bounding the price the rule may set.
     *
     * @param marketplaceId the marketplace the rule applies on
     * @param ruleId        the automatic-pricing rule to assign
     * @param configuration the price-range bound, or {@code null} for none
     */
    public record RuleAssignment(String marketplaceId, String ruleId,
            @Nullable PriceRange configuration) {
    }

    /**
     * A price-range bound for an assigned rule — the minimum and maximum price the
     * rule may set, and the currency the bounds are expressed in.
     */
    public static final class PriceRange {

        /** Which currency the {@link PriceRange} bounds are expressed in. */
        public enum CurrencyBasis {
            /** Bounds are in the offer's base-marketplace currency. */
            BASE_MARKETPLACE_CURRENCY,
            /** Bounds are in the target marketplace's currency. */
            MARKETPLACE_CURRENCY
        }

        private static final String ERR_BASIS = "currency basis must not be null";
        private static final String ERR_MIN_PRICE = "min price must not be null";
        private static final String ERR_MAX_PRICE = "max price must not be null";

        private final CurrencyBasis currencyBasis;
        private final Money minPrice;
        private final Money maxPrice;

        private PriceRange(CurrencyBasis currencyBasis, Money minPrice, Money maxPrice) {
            this.currencyBasis = currencyBasis;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }

        /** A price range bounding the rule between {@code minPrice} and {@code maxPrice}. */
        public static PriceRange of(CurrencyBasis currencyBasis, Money minPrice, Money maxPrice) {
            return new PriceRange(Objects.requireNonNull(currencyBasis, ERR_BASIS),
                    Objects.requireNonNull(minPrice, ERR_MIN_PRICE),
                    Objects.requireNonNull(maxPrice, ERR_MAX_PRICE));
        }

        /** Which currency the bounds are expressed in. */
        public CurrencyBasis currencyBasis() {
            return currencyBasis;
        }

        /** The minimum price the rule may set. */
        public Money minPrice() {
            return minPrice;
        }

        /** The maximum price the rule may set. */
        public Money maxPrice() {
            return maxPrice;
        }
    }
}
