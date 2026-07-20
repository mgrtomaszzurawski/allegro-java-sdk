/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationFixedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationGainRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationPercentageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBulkModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBulkModificationStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockModificationFixedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockModificationGainRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One offer's price and/or stock change in a bulk modification — the unit passed
 * to {@code offers().batch().modifyPricesAndStock(...)}. Set a per-marketplace
 * price change (its Buy Now price), a stock change, or both; at least one is
 * required. Prices are keyed by marketplace id ({@code "allegro-pl"},
 * {@code "allegro-cz"}, …) with at most one change per marketplace.
 *
 * <p>Each change is FIXED (set to a value), GAIN (add to / subtract from the
 * current value) or, for price only, PERCENTAGE (adjust by a percent).
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
    private final StockChange stock;

    private BulkPriceStockModification(Builder builder) {
        this.offerId = builder.offerId;
        this.prices = Map.copyOf(builder.prices);
        this.stock = builder.stock;
    }

    /** Start building the modification for {@code offerId}. */
    public static Builder forOffer(String offerId) {
        return new Builder(offerId);
    }

    /**
     * The generated request element(s) for this offer's modification. Allegro
     * requires each {@code modifications[]} entry to carry <em>exactly one</em> of
     * {@code prices} or {@code stock} (live-verified: a combined element is
     * rejected with {@code INVALID_SINGLE_ELEMENT_IN_MODIFICATION}), so an offer
     * that changes both is emitted as two entries with the same {@code offerId}.
     */
    public List<OfferBulkModificationRaw> toWireElements() {
        List<OfferBulkModificationRaw> elements = new ArrayList<>();
        if (!prices.isEmpty()) {
            Map<String, MarketplacePriceModificationRaw> rawPrices = new LinkedHashMap<>();
            prices.forEach((marketplace, change) -> rawPrices.put(marketplace, change.toRaw()));
            elements.add(new OfferBulkModificationRaw().offerId(offerId).prices(rawPrices));
        }
        if (stock != null) {
            elements.add(new OfferBulkModificationRaw().offerId(offerId).stock(stock.toRaw()));
        }
        return elements;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private final String offerId;
        private final Map<String, PriceChange> prices = new LinkedHashMap<>();
        private StockChange stock;

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
     * ({@link #percentage}). The concrete change type is emitted on the wire from
     * the request DTO's discriminator.
     */
    public static final class PriceChange {

        private enum Kind { FIXED, GAIN, PERCENTAGE }

        private static final String ERR_AMOUNT = "amount must not be null";
        private static final String ERR_PERCENTAGE = "percentage must not be null or blank";

        private final Kind kind;
        private final Money amount;
        private final String percentage;

        private PriceChange(Kind kind, Money amount, String percentage) {
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

        private MarketplacePriceModificationRaw toRaw() {
            return switch (kind) {
                case FIXED -> new MarketplacePriceModificationFixedRaw().value(price(amount));
                case GAIN -> new MarketplacePriceModificationGainRaw().value(price(amount));
                case PERCENTAGE -> new MarketplacePriceModificationPercentageRaw().percentage(percentage);
            };
        }

        private static PriceRaw price(Money money) {
            return new PriceRaw().amount(money.amount()).currency(money.currency());
        }
    }

    /**
     * A stock change — set the stock ({@link #fixed}) or add/subtract a value
     * ({@link #gain}). The concrete change type is emitted from the request DTO's
     * discriminator.
     */
    public static final class StockChange {

        private enum Kind { FIXED, GAIN }

        private final Kind kind;
        private final int value;

        private StockChange(Kind kind, int value) {
            this.kind = kind;
            this.value = value;
        }

        /** Set the available stock to {@code value} (must be positive). */
        public static StockChange fixed(int value) {
            return new StockChange(Kind.FIXED, value);
        }

        /** Add {@code value} to (or, with a negative value, subtract from) the current stock. */
        public static StockChange gain(int value) {
            return new StockChange(Kind.GAIN, value);
        }

        private OfferBulkModificationStockRaw toRaw() {
            return kind == Kind.FIXED
                    ? new StockModificationFixedRaw().value(value)
                    : new StockModificationGainRaw().value(value);
        }
    }
}
