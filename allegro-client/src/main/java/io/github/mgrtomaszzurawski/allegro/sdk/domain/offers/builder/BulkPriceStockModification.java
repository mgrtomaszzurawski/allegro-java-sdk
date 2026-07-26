/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One offer's price and/or stock change in a bulk modification — the unit passed
 * to {@code offers().batch().modifyPricesAndStock(...)}. Set a per-marketplace
 * price change (its Buy Now price), a stock change, or both; at least one is
 * required. Prices are keyed by marketplace id ({@code "allegro-pl"},
 * {@code "allegro-cz"}, …) with at most one change per marketplace.
 *
 * <p>Each change is FIXED (set to a value), GAIN (add to / subtract from the
 * current value) or, for price only, PERCENTAGE (adjust by a percent). The
 * mapping to the wire — including splitting an offer that changes both a price
 * and the stock into the two elements Allegro requires — is the SDK's job; this
 * type only carries the consumer's intent.
 *
 * @since 0.5.0
 */
public final class BulkPriceStockModification {

    private static final String ERR_OFFER_ID = "offerId must not be null or blank";
    private static final String ERR_MARKETPLACE = "marketplace must not be null or blank";
    private static final String ERR_PRICE_CHANGE = "price change must not be null";
    private static final String ERR_STOCK_CHANGE = "stock change must not be null";
    private static final String ERR_EMPTY = "a modification must change at least a price or the stock";

    private final String offerId;
    private final Map<String, PriceChange> prices;
    private final @Nullable StockChange stock;

    private BulkPriceStockModification(Builder builder) {
        this.offerId = builder.offerId;
        this.prices = Map.copyOf(builder.prices);
        this.stock = builder.stock;
    }

    /** Start building the modification for {@code offerId}. */
    public static Builder forOffer(String offerId) {
        return new Builder(offerId);
    }

    /** The offer this modification targets. */
    public String offerId() {
        return offerId;
    }

    /** The per-marketplace price changes (marketplace id → change); empty if none. */
    public Map<String, PriceChange> prices() {
        return prices;
    }

    /** The stock change, or {@code null} if this modification leaves the stock unchanged. */
    public @Nullable StockChange stock() {
        return stock;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private final String offerId;
        private final Map<String, PriceChange> prices = new LinkedHashMap<>();
        private @Nullable StockChange stock;

        private Builder(String offerId) {
            this.offerId = requireText(offerId, ERR_OFFER_ID);
        }

        /** Change the Buy Now price on {@code marketplace} (e.g. {@code "allegro-pl"}). */
        public Builder price(String marketplace, PriceChange change) {
            prices.put(requireText(marketplace, ERR_MARKETPLACE),
                    Objects.requireNonNull(change, ERR_PRICE_CHANGE));
            return this;
        }

        /** Change the available stock. */
        public Builder stock(StockChange change) {
            this.stock = Objects.requireNonNull(change, ERR_STOCK_CHANGE);
            return this;
        }

        /** Build, requiring at least one price or stock change. */
        public BulkPriceStockModification build() {
            if (prices.isEmpty() && stock == null) {
                throw new IllegalStateException(ERR_EMPTY);
            }
            return new BulkPriceStockModification(this);
        }

        private static String requireText(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(message);
            }
            return value;
        }
    }

    /**
     * A Buy Now price change for one marketplace — set the price ({@link #fixed}),
     * add/subtract an amount ({@link #gain}), or adjust by a percent
     * ({@link #percentage}). {@link #kind()} disambiguates the three.
     */
    public static final class PriceChange {

        /** Which kind of price change this is. */
        public enum Kind {
            /** Set the Buy Now price to a fixed amount. */
            FIXED,
            /** Add an amount to (or subtract from) the current price. */
            GAIN,
            /** Adjust the current price by a percent. */
            PERCENTAGE
        }

        private static final String ERR_AMOUNT = "amount must not be null";
        private static final String ERR_PERCENTAGE = "percentage must not be null or blank";

        private final Kind kind;
        private final @Nullable Money amount;
        private final @Nullable String percentage;

        private PriceChange(Kind kind, @Nullable Money amount, @Nullable String percentage) {
            this.kind = kind;
            this.amount = amount;
            this.percentage = percentage;
        }

        /** Set the Buy Now price to {@code amount}. */
        public static PriceChange fixed(Money amount) {
            return new PriceChange(Kind.FIXED, Objects.requireNonNull(amount, ERR_AMOUNT), null);
        }

        /** Add {@code amount} to (or, with a negative amount, subtract from) the current price. */
        public static PriceChange gain(Money amount) {
            return new PriceChange(Kind.GAIN, Objects.requireNonNull(amount, ERR_AMOUNT), null);
        }

        /**
         * Adjust the current price by a percent (e.g. {@code "10%"}, {@code "33.3"},
         * {@code "-10.50%"}); {@code +}/{@code %} are optional and redundant.
         */
        public static PriceChange percentage(String percentage) {
            if (percentage == null || percentage.isBlank()) {
                throw new IllegalArgumentException(ERR_PERCENTAGE);
            }
            return new PriceChange(Kind.PERCENTAGE, null, percentage);
        }

        /** Which kind of change this is. */
        public Kind kind() {
            return kind;
        }

        /** The amount for a {@link Kind#FIXED}/{@link Kind#GAIN} change; {@code null} for PERCENTAGE. */
        public @Nullable Money amount() {
            return amount;
        }

        /** The percent string for a {@link Kind#PERCENTAGE} change; {@code null} otherwise. */
        public @Nullable String percentage() {
            return percentage;
        }
    }

    /**
     * A stock change — set the stock ({@link #fixed}) or add/subtract a value
     * ({@link #gain}). {@link #kind()} disambiguates the two.
     */
    public static final class StockChange {

        /** Which kind of stock change this is. */
        public enum Kind {
            /** Set the available stock to a fixed value. */
            FIXED,
            /** Add a value to (or subtract from) the current stock. */
            GAIN
        }

        private static final String ERR_FIXED_POSITIVE = "fixed stock must be positive";

        private final Kind kind;
        private final int value;

        private StockChange(Kind kind, int value) {
            this.kind = kind;
            this.value = value;
        }

        /** Set the available stock to {@code value} (must be positive). */
        public static StockChange fixed(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(ERR_FIXED_POSITIVE);
            }
            return new StockChange(Kind.FIXED, value);
        }

        /** Add {@code value} to (or, with a negative value, subtract from) the current stock. */
        public static StockChange gain(int value) {
            return new StockChange(Kind.GAIN, value);
        }

        /** Which kind of change this is. */
        public Kind kind() {
            return kind;
        }

        /** The stock value: the new stock for FIXED, the delta for GAIN. */
        public int value() {
            return value;
        }
    }
}
