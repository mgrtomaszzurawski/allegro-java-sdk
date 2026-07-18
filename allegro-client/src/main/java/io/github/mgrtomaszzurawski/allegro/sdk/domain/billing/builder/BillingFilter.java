/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming the seller's billing entries. All fields are
 * optional; {@link #all()} streams every entry.
 *
 * @since 0.5.0
 */
public final class BillingFilter {

    private final @Nullable String marketplaceId;
    private final @Nullable OffsetDateTime occurredFrom;
    private final @Nullable OffsetDateTime occurredTo;
    private final @Nullable String typeId;
    private final @Nullable String offerId;
    private final @Nullable String orderId;

    private BillingFilter(Builder builder) {
        this.marketplaceId = builder.marketplaceId;
        this.occurredFrom = builder.occurredFrom;
        this.occurredTo = builder.occurredTo;
        this.typeId = builder.typeId;
        this.offerId = builder.offerId;
        this.orderId = builder.orderId;
    }

    /** Marketplace to match, or {@code null}. */
    public @Nullable String marketplaceId() {
        return marketplaceId;
    }

    /** Lower bound (inclusive) on the entry time, or {@code null}. */
    public @Nullable OffsetDateTime occurredFrom() {
        return occurredFrom;
    }

    /** Upper bound (inclusive) on the entry time, or {@code null}. */
    public @Nullable OffsetDateTime occurredTo() {
        return occurredTo;
    }

    /** Billing type id to match, or {@code null}. */
    public @Nullable String typeId() {
        return typeId;
    }

    /** Offer id to match, or {@code null}. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** Order id to match, or {@code null}. */
    public @Nullable String orderId() {
        return orderId;
    }

    /** A filter that streams every billing entry. */
    public static BillingFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .marketplaceId(marketplaceId)
                .occurredFrom(occurredFrom)
                .occurredTo(occurredTo)
                .typeId(typeId)
                .offerId(offerId)
                .orderId(orderId);
    }

    /** Fluent builder for {@link BillingFilter}. */
    public static final class Builder {

        private @Nullable String marketplaceId;
        private @Nullable OffsetDateTime occurredFrom;
        private @Nullable OffsetDateTime occurredTo;
        private @Nullable String typeId;
        private @Nullable String offerId;
        private @Nullable String orderId;

        /** Keep entries booked on this marketplace. */
        public Builder marketplaceId(@Nullable String value) {
            this.marketplaceId = value;
            return this;
        }

        /** Keep entries booked at or after this instant. */
        public Builder occurredFrom(@Nullable OffsetDateTime value) {
            this.occurredFrom = value;
            return this;
        }

        /** Keep entries booked at or before this instant. */
        public Builder occurredTo(@Nullable OffsetDateTime value) {
            this.occurredTo = value;
            return this;
        }

        /** Keep entries of this billing type. */
        public Builder typeId(@Nullable String value) {
            this.typeId = value;
            return this;
        }

        /** Keep entries related to this offer. */
        public Builder offerId(@Nullable String value) {
            this.offerId = value;
            return this;
        }

        /** Keep entries related to this order. */
        public Builder orderId(@Nullable String value) {
            this.orderId = value;
            return this;
        }

        /** Build the filter. */
        public BillingFilter build() {
            return new BillingFilter(this);
        }
    }
}
