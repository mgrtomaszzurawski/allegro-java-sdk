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
 * price ({@link Builder#setPrice}), raise the current price by an amount
 * ({@link Builder#increaseBy}), or lower it by an amount ({@link Builder#decreaseBy}).
 * The change may target a specific marketplace ({@link Builder#onMarketplace}); when
 * omitted, Allegro changes the price on each offer's base marketplace.
 *
 * <p>Exactly one of set / increase / decrease is required; the fixed price must be
 * positive and an increase/decrease amount must be non-negative.
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
    private static final String ERR_PRICE_POSITIVE = "price amount must be positive: ";
    private static final String ERR_AMOUNT_NON_NEGATIVE = "change amount must not be negative: ";
    private static final String ERR_MARKETPLACE = "marketplace id must not be null or blank";
    private static final String ERR_SINGLE_CHANGE =
            "a price change sets exactly one of a fixed price, an increase, or a decrease";
    private static final String ERR_NO_CHANGE =
            "a price change must set a fixed price, an increase, or a decrease";

    /** How the target price is derived. */
    public enum Kind {
        /** Set an absolute Buy Now price. */
        FIXED,
        /** Raise the current price by an amount. */
        INCREASE,
        /** Lower the current price by an amount. */
        DECREASE
    }

    private final List<String> offerIds;
    private final Kind kind;
    private final Money amount;
    private final @Nullable String marketplaceId;

    private PriceChangeRequest(Builder builder) {
        this.offerIds = List.copyOf(builder.offerIds);
        this.kind = Objects.requireNonNull(builder.kind, ERR_NO_CHANGE);
        this.amount = Objects.requireNonNull(builder.amount, ERR_NO_CHANGE);
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

    /** How the target price is derived (fixed / increase / decrease). */
    public Kind kind() {
        return kind;
    }

    /** The fixed price (for {@link Kind#FIXED}) or the increase/decrease amount. */
    public Money amount() {
        return amount;
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
        private @Nullable String marketplaceId;

        private Builder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Set an absolute Buy Now price (the request's single change). Must be positive. */
        public Builder setPrice(Money price) {
            return change(Kind.FIXED, requirePositive(price));
        }

        /** Raise the current price by {@code amount} (the request's single change). */
        public Builder increaseBy(Money amount) {
            return change(Kind.INCREASE, requireNonNegative(amount));
        }

        /** Lower the current price by {@code amount} (the request's single change). */
        public Builder decreaseBy(Money amount) {
            return change(Kind.DECREASE, requireNonNegative(amount));
        }

        /** Target a specific marketplace; when unset, the offer's base marketplace is used. */
        public Builder onMarketplace(String marketplaceId) {
            if (marketplaceId == null || marketplaceId.isBlank()) {
                throw new IllegalArgumentException(ERR_MARKETPLACE);
            }
            this.marketplaceId = marketplaceId;
            return this;
        }

        /** Build, requiring exactly one of set / increase / decrease. */
        public PriceChangeRequest build() {
            if (kind == null) {
                throw new IllegalStateException(ERR_NO_CHANGE);
            }
            return new PriceChangeRequest(this);
        }

        private Builder change(Kind newKind, Money changeAmount) {
            if (kind != null) {
                throw new IllegalStateException(ERR_SINGLE_CHANGE);
            }
            this.kind = newKind;
            this.amount = changeAmount;
            return this;
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
