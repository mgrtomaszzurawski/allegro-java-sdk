/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming the seller's payment-operations history. All
 * fields are optional; {@link #all()} streams every operation.
 *
 * @since 0.5.0
 */
public final class PaymentOperationFilter {

    private final @Nullable String paymentId;
    private final @Nullable String participantLogin;
    private final @Nullable OffsetDateTime occurredFrom;
    private final @Nullable OffsetDateTime occurredTo;
    private final @Nullable String group;
    private final @Nullable String marketplaceId;
    private final @Nullable String currency;

    private PaymentOperationFilter(Builder builder) {
        this.paymentId = builder.paymentId;
        this.participantLogin = builder.participantLogin;
        this.occurredFrom = builder.occurredFrom;
        this.occurredTo = builder.occurredTo;
        this.group = builder.group;
        this.marketplaceId = builder.marketplaceId;
        this.currency = builder.currency;
    }

    /** Payment id to match, or {@code null}. */
    public @Nullable String paymentId() {
        return paymentId;
    }

    /** Participant login to match, or {@code null}. */
    public @Nullable String participantLogin() {
        return participantLogin;
    }

    /** Lower bound (inclusive) on the operation time, or {@code null}. */
    public @Nullable OffsetDateTime occurredFrom() {
        return occurredFrom;
    }

    /** Upper bound (inclusive) on the operation time, or {@code null}. */
    public @Nullable OffsetDateTime occurredTo() {
        return occurredTo;
    }

    /** Wallet group to match ({@code INCOME}/{@code OUTCOME}/…), or {@code null}. */
    public @Nullable String group() {
        return group;
    }

    /** Marketplace id to match, or {@code null}. */
    public @Nullable String marketplaceId() {
        return marketplaceId;
    }

    /** Currency to match, or {@code null}. */
    public @Nullable String currency() {
        return currency;
    }

    /** A filter that streams every payment operation. */
    public static PaymentOperationFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .paymentId(paymentId)
                .participantLogin(participantLogin)
                .occurredFrom(occurredFrom)
                .occurredTo(occurredTo)
                .group(group)
                .marketplaceId(marketplaceId)
                .currency(currency);
    }

    /** Fluent builder for {@link PaymentOperationFilter}. */
    public static final class Builder {

        private @Nullable String paymentId;
        private @Nullable String participantLogin;
        private @Nullable OffsetDateTime occurredFrom;
        private @Nullable OffsetDateTime occurredTo;
        private @Nullable String group;
        private @Nullable String marketplaceId;
        private @Nullable String currency;

        /** Keep operations for this payment id. */
        public Builder paymentId(@Nullable String value) {
            this.paymentId = value;
            return this;
        }

        /** Keep operations for this participant login. */
        public Builder participantLogin(@Nullable String value) {
            this.participantLogin = value;
            return this;
        }

        /** Keep operations at or after this instant. */
        public Builder occurredFrom(@Nullable OffsetDateTime value) {
            this.occurredFrom = value;
            return this;
        }

        /** Keep operations at or before this instant. */
        public Builder occurredTo(@Nullable OffsetDateTime value) {
            this.occurredTo = value;
            return this;
        }

        /** Keep operations of this wallet group. */
        public Builder group(@Nullable String value) {
            this.group = value;
            return this;
        }

        /** Keep operations on this marketplace. */
        public Builder marketplaceId(@Nullable String value) {
            this.marketplaceId = value;
            return this;
        }

        /** Keep operations in this currency. */
        public Builder currency(@Nullable String value) {
            this.currency = value;
            return this;
        }

        /** Build the filter. */
        public PaymentOperationFilter build() {
            return new PaymentOperationFilter(this);
        }
    }
}
