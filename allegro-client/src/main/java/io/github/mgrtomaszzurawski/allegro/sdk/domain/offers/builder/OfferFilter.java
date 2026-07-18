/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming a seller's offers. All fields are optional;
 * {@link #all()} matches every offer.
 *
 * <pre>{@code
 * OfferFilter activeBuyNow = OfferFilter.builder()
 *         .name("keyboard")
 *         .status(OfferStatus.ACTIVE)
 *         .format(OfferFormat.BUY_NOW)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class OfferFilter {

    private final @Nullable String name;
    private final @Nullable OfferStatus status;
    private final @Nullable OfferFormat format;
    private final @Nullable String priceFrom;
    private final @Nullable String priceTo;

    private OfferFilter(Builder builder) {
        this.name = builder.name;
        this.status = builder.status;
        this.format = builder.format;
        this.priceFrom = builder.priceFrom;
        this.priceTo = builder.priceTo;
    }

    /** Substring the offer name must contain, or {@code null} for no name filter. */
    public @Nullable String name() {
        return name;
    }

    /** Publication status to keep, or {@code null} for every status. */
    public @Nullable OfferStatus status() {
        return status;
    }

    /** Selling format to keep, or {@code null} for every format. */
    public @Nullable OfferFormat format() {
        return format;
    }

    /** Lower bound (inclusive) on the Buy Now price amount, or {@code null}. */
    public @Nullable String priceFrom() {
        return priceFrom;
    }

    /** Upper bound (inclusive) on the Buy Now price amount, or {@code null}. */
    public @Nullable String priceTo() {
        return priceTo;
    }

    /** A filter that matches every offer. */
    public static OfferFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .status(status)
                .format(format)
                .priceFrom(priceFrom)
                .priceTo(priceTo);
    }

    /** Fluent builder for {@link OfferFilter}. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable OfferStatus status;
        private @Nullable OfferFormat format;
        private @Nullable String priceFrom;
        private @Nullable String priceTo;

        /** Keep only offers whose name contains this text. */
        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /** Keep only offers in this publication status. */
        public Builder status(@Nullable OfferStatus status) {
            this.status = status;
            return this;
        }

        /** Keep only offers sold in this format. */
        public Builder format(@Nullable OfferFormat format) {
            this.format = format;
            return this;
        }

        /** Keep only offers priced at or above this amount (exact decimal, e.g. {@code "10.00"}). */
        public Builder priceFrom(@Nullable String priceFrom) {
            this.priceFrom = priceFrom;
            return this;
        }

        /** Keep only offers priced at or below this amount (exact decimal, e.g. {@code "500.00"}). */
        public Builder priceTo(@Nullable String priceTo) {
            this.priceTo = priceTo;
            return this;
        }

        /** Build the filter. */
        public OfferFilter build() {
            return new OfferFilter(this);
        }
    }
}
