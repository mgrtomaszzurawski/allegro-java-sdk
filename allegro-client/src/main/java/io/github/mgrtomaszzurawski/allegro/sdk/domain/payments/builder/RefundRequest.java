/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.RefundReason;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A request to initiate a full refund of a payment via
 * {@code payments().refund(...)}. The payment id, order id, an idempotency
 * {@code commandId} and a {@link RefundReason} are all required; the payment and
 * order ids must be UUIDs.
 *
 * <p>Reuse the same {@code commandId} to retry an interrupted refund safely — the
 * server treats a repeat as the same operation, not a second refund.
 *
 * <pre>{@code
 * RefundRequest refund = RefundRequest.builder()
 *         .paymentId(paymentId)
 *         .orderId(orderId)
 *         .commandId(UUID.randomUUID().toString())
 *         .reason(RefundReason.COMPLAINT)
 *         .build();
 * }</pre>
 *
 * @since 0.5.0
 */
public final class RefundRequest {

    private static final String ERR_PAYMENT_ID = "paymentId is required";
    private static final String ERR_PAYMENT_ID_UUID = "paymentId must be a UUID: ";
    private static final String ERR_ORDER_ID = "orderId is required";
    private static final String ERR_ORDER_ID_UUID = "orderId must be a UUID: ";
    private static final String ERR_COMMAND_ID = "commandId is required";
    private static final String ERR_REASON = "reason is required";

    private final String paymentId;
    private final String orderId;
    private final String commandId;
    private final RefundReason reason;

    private RefundRequest(Builder builder) {
        this.paymentId = requireUuid(builder.paymentId, ERR_PAYMENT_ID, ERR_PAYMENT_ID_UUID);
        this.orderId = requireUuid(builder.orderId, ERR_ORDER_ID, ERR_ORDER_ID_UUID);
        this.commandId = requireNonBlank(builder.commandId, ERR_COMMAND_ID);
        if (builder.reason == null) {
            throw new IllegalStateException(ERR_REASON);
        }
        this.reason = builder.reason;
    }

    private static String requireNonBlank(@Nullable String value, String missingMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(missingMessage);
        }
        return value;
    }

    private static String requireUuid(@Nullable String value, String missingMessage,
            String notUuidMessage) {
        String present = requireNonBlank(value, missingMessage);
        try {
            UUID.fromString(present);
        } catch (IllegalArgumentException notUuid) {
            throw new IllegalStateException(notUuidMessage + present, notUuid);
        }
        return present;
    }

    /** The refunded payment's UUID id. */
    public String paymentId() {
        return paymentId;
    }

    /** The related order's UUID id. */
    public String orderId() {
        return orderId;
    }

    /** The idempotency command id. */
    public String commandId() {
        return commandId;
    }

    /** Why the refund is being initiated. */
    public RefundReason reason() {
        return reason;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .commandId(commandId)
                .reason(reason);
    }

    /** Fluent builder for {@link RefundRequest}. */
    public static final class Builder {

        private @Nullable String paymentId;
        private @Nullable String orderId;
        private @Nullable String commandId;
        private @Nullable RefundReason reason;

        /** Set the refunded payment's UUID id (required). */
        public Builder paymentId(@Nullable String value) {
            this.paymentId = value;
            return this;
        }

        /** Set the related order's UUID id (required). */
        public Builder orderId(@Nullable String value) {
            this.orderId = value;
            return this;
        }

        /** Set the idempotency command id (required). */
        public Builder commandId(@Nullable String value) {
            this.commandId = value;
            return this;
        }

        /** Set the refund reason (required). */
        public Builder reason(@Nullable RefundReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if a required field is missing, or the
         *     payment/order id is not a UUID
         */
        public RefundRequest build() {
            return new RefundRequest(this);
        }
    }
}
