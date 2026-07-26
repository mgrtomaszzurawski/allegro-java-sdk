/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A bulk Buy Now price change — the request passed to
 * {@code offers().batch().changePrices(...)}. It applies <em>one</em> price change
 * to every target offer (up to {@value #MAX_OFFERS}) in one command: set a fixed
 * price ({@link Builder#setPrice}), raise or lower the current price by an amount
 * ({@link Builder#increaseBy} / {@link Builder#decreaseBy}), or raise or lower it by
 * a percentage ({@link Builder#increaseByPercent} / {@link Builder#decreaseByPercent}).
 * The change may target a specific marketplace ({@link Builder#onMarketplace}); when
 * omitted, Allegro changes the price on each offer's base marketplace.
 *
 * <p>Exactly one change is required; a fixed price must be positive, an increase/decrease
 * amount must be non-negative, and a percentage must be non-blank.
 *
 * @since 0.6.0
 */
public final class PriceChangeRequest {

    /** Allegro accepts up to 1000 offers in one criterion. */
    public static final int MAX_OFFERS = 1000;

    private static final String ERR_OFFERS_EMPTY = "at least one offer id is required";
    private static final String ERR_OFFERS_TOO_MANY = "at most " + MAX_OFFERS + " offers per command";
    private static final String ERR_OFFER_ID = "offer id must not be null or blank";
    private static final String ERR_AMOUNT = "price amount must not be null";
    private static final String ERR_PERCENTAGE = "percentage must not be null or blank";
    private static final String ERR_PRICE_POSITIVE = "price amount must be positive: ";
    private static final String ERR_AMOUNT_NON_NEGATIVE = "change amount must not be negative: ";
    private static final String ERR_MARKETPLACE = "marketplace id must not be null or blank";
    private static final String ERR_SINGLE_CHANGE =
            "a price change sets exactly one of a fixed price, an amount change, or a percentage change";
    private static final String ERR_NO_CHANGE =
            "a price change must set a fixed price, an amount change, or a percentage change";

    /** How the target price is derived. */
    public enum Kind {
        /** Set an absolute Buy Now price. */
        FIXED,
        /** Raise the current price by an amount. */
        INCREASE,
        /** Lower the current price by an amount. */
        DECREASE,
        /** Raise the current price by a percentage. */
        INCREASE_PERCENTAGE,
        /** Lower the current price by a percentage. */
        DECREASE_PERCENTAGE
    }

    private final List<String> offerIds;
    private final Kind kind;
    private final @Nullable Money amount;
    private final @Nullable String percentage;
    private final @Nullable String marketplaceId;

    private PriceChangeRequest(Builder builder) {
        this.offerIds = List.copyOf(builder.offerIds);
        this.kind = Objects.requireNonNull(builder.kind, ERR_NO_CHANGE);
        this.amount = builder.amount;
        this.percentage = builder.percentage;
        this.marketplaceId = builder.marketplaceId;
    }

    /** Start building a price change for {@code offerIds}. */
    public static Builder forOffers(List<String> offerIds) {
        return new Builder(offerIds);
    }

    /** The offers this change targets. */
    public List<String> offerIds() {
        return offerIds;
    }

    /** How the target price is derived (fixed / amount / percentage change). */
    public Kind kind() {
        return kind;
    }

    /** The fixed price or the increase/decrease amount, or {@code null} for a percentage change. */
    public @Nullable Money amount() {
        return amount;
    }

    /** The increase/decrease percentage (e.g. {@code "10"}), or {@code null} for an amount change. */
    public @Nullable String percentage() {
        return percentage;
    }

    /** The marketplace to change the price on, or {@code null} for the offer's base marketplace. */
    public @Nullable String marketplaceId() {
        return marketplaceId;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private final List<String> offerIds;
        private @Nullable Kind kind;
        private @Nullable Money amount;
        private @Nullable String percentage;
        private @Nullable String marketplaceId;

        private Builder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Set an absolute Buy Now price (the request's single change). Must be positive. */
        public Builder setPrice(Money price) {
            return amountChange(Kind.FIXED, requirePositive(price));
        }

        /** Raise the current price by {@code amount} (the request's single change). */
        public Builder increaseBy(Money amount) {
            return amountChange(Kind.INCREASE, requireNonNegative(amount));
        }

        /** Lower the current price by {@code amount} (the request's single change). */
        public Builder decreaseBy(Money amount) {
            return amountChange(Kind.DECREASE, requireNonNegative(amount));
        }

        /** Raise the current price by {@code percentage} (e.g. {@code "10"}); the single change. */
        public Builder increaseByPercent(String percentage) {
            return percentageChange(Kind.INCREASE_PERCENTAGE, percentage);
        }

        /** Lower the current price by {@code percentage} (e.g. {@code "10"}); the single change. */
        public Builder decreaseByPercent(String percentage) {
            return percentageChange(Kind.DECREASE_PERCENTAGE, percentage);
        }

        /** Target a specific marketplace; when unset, the offer's base marketplace is used. */
        public Builder onMarketplace(String marketplaceId) {
            if (marketplaceId == null || marketplaceId.isBlank()) {
                throw new IllegalArgumentException(ERR_MARKETPLACE);
            }
            this.marketplaceId = marketplaceId;
            return this;
        }

        /** Build, requiring exactly one change. */
        public PriceChangeRequest build() {
            if (kind == null) {
                throw new IllegalStateException(ERR_NO_CHANGE);
            }
            return new PriceChangeRequest(this);
        }

        private Builder amountChange(Kind newKind, Money changeAmount) {
            requireNoChangeYet();
            this.kind = newKind;
            this.amount = changeAmount;
            return this;
        }

        private Builder percentageChange(Kind newKind, String changePercentage) {
            requireNoChangeYet();
            if (changePercentage == null || changePercentage.isBlank()) {
                throw new IllegalArgumentException(ERR_PERCENTAGE);
            }
            this.kind = newKind;
            this.percentage = changePercentage;
            return this;
        }

        private void requireNoChangeYet() {
            if (kind != null) {
                throw new IllegalStateException(ERR_SINGLE_CHANGE);
            }
        }

        private static Money requirePositive(Money price) {
            Objects.requireNonNull(price, ERR_AMOUNT);
            if (price.amountAsDecimal().signum() <= 0) {
                throw new IllegalArgumentException(ERR_PRICE_POSITIVE + price.amount());
            }
            return price;
        }

        private static Money requireNonNegative(Money amount) {
            Objects.requireNonNull(amount, ERR_AMOUNT);
            if (amount.amountAsDecimal().signum() < 0) {
                throw new IllegalArgumentException(ERR_AMOUNT_NON_NEGATIVE + amount.amount());
            }
            return amount;
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
                if (offerId == null || offerId.isBlank()) {
                    throw new IllegalArgumentException(ERR_OFFER_ID);
                }
            }
            return offerIds;
        }
    }
}
