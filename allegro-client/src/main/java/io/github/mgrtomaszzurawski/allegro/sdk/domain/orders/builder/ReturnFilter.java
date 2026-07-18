/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming customer returns (BETA). All fields are
 * optional; {@link #all()} streams every return.
 *
 * @since 0.6.0
 */
public final class ReturnFilter {

    private final @Nullable String orderId;
    private final @Nullable String buyerLogin;
    private final @Nullable String buyerEmail;
    private final @Nullable String referenceNumber;
    private final @Nullable OffsetDateTime createdFrom;
    private final @Nullable OffsetDateTime createdTo;

    private ReturnFilter(Builder builder) {
        this.orderId = builder.orderId;
        this.buyerLogin = builder.buyerLogin;
        this.buyerEmail = builder.buyerEmail;
        this.referenceNumber = builder.referenceNumber;
        this.createdFrom = builder.createdFrom;
        this.createdTo = builder.createdTo;
    }

    /** Order id to match, or {@code null}. */
    public @Nullable String orderId() {
        return orderId;
    }

    /** Buyer login to match, or {@code null}. */
    public @Nullable String buyerLogin() {
        return buyerLogin;
    }

    /** Buyer email to match, or {@code null}. */
    public @Nullable String buyerEmail() {
        return buyerEmail;
    }

    /** Reference number to match, or {@code null}. */
    public @Nullable String referenceNumber() {
        return referenceNumber;
    }

    /** Lower bound (inclusive) on the return's creation time, or {@code null}. */
    public @Nullable OffsetDateTime createdFrom() {
        return createdFrom;
    }

    /** Upper bound (inclusive) on the return's creation time, or {@code null}. */
    public @Nullable OffsetDateTime createdTo() {
        return createdTo;
    }

    /** A filter that streams every customer return. */
    public static ReturnFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .orderId(orderId)
                .buyerLogin(buyerLogin)
                .buyerEmail(buyerEmail)
                .referenceNumber(referenceNumber)
                .createdFrom(createdFrom)
                .createdTo(createdTo);
    }

    /** Fluent builder for {@link ReturnFilter}. */
    public static final class Builder {

        private @Nullable String orderId;
        private @Nullable String buyerLogin;
        private @Nullable String buyerEmail;
        private @Nullable String referenceNumber;
        private @Nullable OffsetDateTime createdFrom;
        private @Nullable OffsetDateTime createdTo;

        /** Keep returns against this order. */
        public Builder orderId(@Nullable String value) {
            this.orderId = value;
            return this;
        }

        /** Keep returns by this buyer login. */
        public Builder buyerLogin(@Nullable String value) {
            this.buyerLogin = value;
            return this;
        }

        /** Keep returns by this buyer email. */
        public Builder buyerEmail(@Nullable String value) {
            this.buyerEmail = value;
            return this;
        }

        /** Keep the return with this reference number. */
        public Builder referenceNumber(@Nullable String value) {
            this.referenceNumber = value;
            return this;
        }

        /** Keep returns created at or after this instant. */
        public Builder createdFrom(@Nullable OffsetDateTime value) {
            this.createdFrom = value;
            return this;
        }

        /** Keep returns created at or before this instant. */
        public Builder createdTo(@Nullable OffsetDateTime value) {
            this.createdTo = value;
            return this;
        }

        /** Build the filter. */
        public ReturnFilter build() {
            return new ReturnFilter(this);
        }
    }
}
