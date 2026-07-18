/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming refunded payments. All fields are optional;
 * {@link #all()} streams every refund.
 *
 * @since 0.5.0
 */
public final class RefundFilter {

    private final @Nullable String refundId;
    private final @Nullable String paymentId;
    private final @Nullable String orderId;
    private final @Nullable OffsetDateTime occurredFrom;
    private final @Nullable OffsetDateTime occurredTo;
    private final @Nullable String status;

    private RefundFilter(Builder builder) {
        this.refundId = builder.refundId;
        this.paymentId = builder.paymentId;
        this.orderId = builder.orderId;
        this.occurredFrom = builder.occurredFrom;
        this.occurredTo = builder.occurredTo;
        this.status = builder.status;
    }

    /** Refund id to match, or {@code null}. */
    public @Nullable String refundId() {
        return refundId;
    }

    /** Payment id to match, or {@code null}. */
    public @Nullable String paymentId() {
        return paymentId;
    }

    /** Order id to match, or {@code null}. */
    public @Nullable String orderId() {
        return orderId;
    }

    /** Lower bound (inclusive) on the refund time, or {@code null}. */
    public @Nullable OffsetDateTime occurredFrom() {
        return occurredFrom;
    }

    /** Upper bound (inclusive) on the refund time, or {@code null}. */
    public @Nullable OffsetDateTime occurredTo() {
        return occurredTo;
    }

    /** Refund status to match, or {@code null}. */
    public @Nullable String status() {
        return status;
    }

    /** A filter that streams every refund. */
    public static RefundFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .orderId(orderId)
                .occurredFrom(occurredFrom)
                .occurredTo(occurredTo)
                .status(status);
    }

    /** Fluent builder for {@link RefundFilter}. */
    public static final class Builder {

        private @Nullable String refundId;
        private @Nullable String paymentId;
        private @Nullable String orderId;
        private @Nullable OffsetDateTime occurredFrom;
        private @Nullable OffsetDateTime occurredTo;
        private @Nullable String status;

        /** Keep the refund with this id. */
        public Builder refundId(@Nullable String value) {
            this.refundId = value;
            return this;
        }

        /** Keep refunds of this payment. */
        public Builder paymentId(@Nullable String value) {
            this.paymentId = value;
            return this;
        }

        /** Keep refunds of this order. */
        public Builder orderId(@Nullable String value) {
            this.orderId = value;
            return this;
        }

        /** Keep refunds at or after this instant. */
        public Builder occurredFrom(@Nullable OffsetDateTime value) {
            this.occurredFrom = value;
            return this;
        }

        /** Keep refunds at or before this instant. */
        public Builder occurredTo(@Nullable OffsetDateTime value) {
            this.occurredTo = value;
            return this;
        }

        /** Keep refunds with this status. */
        public Builder status(@Nullable String value) {
            this.status = value;
            return this;
        }

        /** Build the filter. */
        public RefundFilter build() {
            return new RefundFilter(this);
        }
    }
}
