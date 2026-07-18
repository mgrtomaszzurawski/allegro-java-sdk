/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming the fulfillment stock report
 * ({@code fulfillment().stock(filter)}). All fields are optional; {@link #all()}
 * walks the entire stock.
 *
 * <p>The status filters ({@code productAvailability}, {@code productStatus},
 * {@code asnStatus}) and {@code sort} are passed through as the exact tokens
 * Allegro documents for the {@code /fulfillment/stock} query, so new server-side
 * values need no SDK change.
 *
 * <pre>{@code
 * StockFilter lowStock = StockFilter.builder()
 *         .phrase("headphones")
 *         .outOfStockInTo(14)
 *         .build();
 * }</pre>
 *
 * @since 0.3.0
 */
public final class StockFilter {

    private final @Nullable String phrase;
    private final @Nullable String sort;
    private final @Nullable String productId;
    private final @Nullable String productAvailability;
    private final @Nullable String productStatus;
    private final @Nullable String asnStatus;
    private final @Nullable Integer outOfStockInFrom;
    private final @Nullable Integer outOfStockInTo;

    private StockFilter(Builder builder) {
        this.phrase = builder.phrase;
        this.sort = builder.sort;
        this.productId = builder.productId;
        this.productAvailability = builder.productAvailability;
        this.productStatus = builder.productStatus;
        this.asnStatus = builder.asnStatus;
        this.outOfStockInFrom = builder.outOfStockInFrom;
        this.outOfStockInTo = builder.outOfStockInTo;
    }

    /** Free-text phrase matched against product name / codes, or {@code null}. */
    public @Nullable String phrase() {
        return phrase;
    }

    /** Sort expression for the report, or {@code null} for the server default. */
    public @Nullable String sort() {
        return sort;
    }

    /** Restrict to a single Allegro product id, or {@code null}. */
    public @Nullable String productId() {
        return productId;
    }

    /** Product-availability token, or {@code null}. */
    public @Nullable String productAvailability() {
        return productAvailability;
    }

    /** Product-status token, or {@code null}. */
    public @Nullable String productStatus() {
        return productStatus;
    }

    /** Advance-ship-notice status token, or {@code null}. */
    public @Nullable String asnStatus() {
        return asnStatus;
    }

    /** Lower bound (inclusive) on estimated days until out of stock, or {@code null}. */
    public @Nullable Integer outOfStockInFrom() {
        return outOfStockInFrom;
    }

    /** Upper bound (inclusive) on estimated days until out of stock, or {@code null}. */
    public @Nullable Integer outOfStockInTo() {
        return outOfStockInTo;
    }

    /** A filter that walks the entire stock. */
    public static StockFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .phrase(phrase)
                .sort(sort)
                .productId(productId)
                .productAvailability(productAvailability)
                .productStatus(productStatus)
                .asnStatus(asnStatus)
                .outOfStockInFrom(outOfStockInFrom)
                .outOfStockInTo(outOfStockInTo);
    }

    /** Fluent builder for {@link StockFilter}. */
    public static final class Builder {

        private @Nullable String phrase;
        private @Nullable String sort;
        private @Nullable String productId;
        private @Nullable String productAvailability;
        private @Nullable String productStatus;
        private @Nullable String asnStatus;
        private @Nullable Integer outOfStockInFrom;
        private @Nullable Integer outOfStockInTo;

        /** Match a free-text phrase against product name / codes. */
        public Builder phrase(@Nullable String phrase) {
            this.phrase = phrase;
            return this;
        }

        /** Sort the report by this server-side sort expression. */
        public Builder sort(@Nullable String sort) {
            this.sort = sort;
            return this;
        }

        /** Restrict to a single Allegro product id. */
        public Builder productId(@Nullable String productId) {
            this.productId = productId;
            return this;
        }

        /** Restrict by product-availability token. */
        public Builder productAvailability(@Nullable String productAvailability) {
            this.productAvailability = productAvailability;
            return this;
        }

        /** Restrict by product-status token. */
        public Builder productStatus(@Nullable String productStatus) {
            this.productStatus = productStatus;
            return this;
        }

        /** Restrict by advance-ship-notice status token. */
        public Builder asnStatus(@Nullable String asnStatus) {
            this.asnStatus = asnStatus;
            return this;
        }

        /** Keep products estimated to run out in at least this many days. */
        public Builder outOfStockInFrom(@Nullable Integer outOfStockInFrom) {
            this.outOfStockInFrom = outOfStockInFrom;
            return this;
        }

        /** Keep products estimated to run out in at most this many days. */
        public Builder outOfStockInTo(@Nullable Integer outOfStockInTo) {
            this.outOfStockInTo = outOfStockInTo;
            return this;
        }

        /** Build the filter. */
        public StockFilter build() {
            return new StockFilter(this);
        }
    }
}
