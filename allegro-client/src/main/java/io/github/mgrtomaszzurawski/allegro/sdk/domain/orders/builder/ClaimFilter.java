/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming commission-refund claims. All fields are
 * optional; {@link #all()} streams every claim.
 *
 * @since 0.6.0
 */
public final class ClaimFilter {

    private final @Nullable String offerId;
    private final @Nullable String buyerId;
    private final @Nullable String status;

    private ClaimFilter(Builder builder) {
        this.offerId = builder.offerId;
        this.buyerId = builder.buyerId;
        this.status = builder.status;
    }

    /** Offer id to match, or {@code null}. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** Buyer id to match, or {@code null}. */
    public @Nullable String buyerId() {
        return buyerId;
    }

    /** Claim status to match, or {@code null}. */
    public @Nullable String status() {
        return status;
    }

    /** A filter that streams every claim. */
    public static ClaimFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().offerId(offerId).buyerId(buyerId).status(status);
    }

    /** Fluent builder for {@link ClaimFilter}. */
    public static final class Builder {

        private @Nullable String offerId;
        private @Nullable String buyerId;
        private @Nullable String status;

        /** Keep claims for this offer. */
        public Builder offerId(@Nullable String value) {
            this.offerId = value;
            return this;
        }

        /** Keep claims for line items sold to this buyer. */
        public Builder buyerId(@Nullable String value) {
            this.buyerId = value;
            return this;
        }

        /** Keep claims with this status. */
        public Builder status(@Nullable String value) {
            this.status = value;
            return this;
        }

        /** Build the filter. */
        public ClaimFilter build() {
            return new ClaimFilter(this);
        }
    }
}
