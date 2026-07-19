/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.RefundReason;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A request to initiate a refund of a payment via {@code payments().refund(...)}.
 * The payment id, order id, an idempotency {@code commandId} and a
 * {@link RefundReason} are all required; the payment and order ids must be UUIDs.
 *
 * <p>Reuse the same {@code commandId} to retry an interrupted refund safely — the
 * server treats a repeat as the same operation, not a second refund.
 *
 * <p>With only the required fields set, the whole payment is refunded. To refund
 * <em>part</em> of a payment, add the components to refund — per line item
 * ({@link RefundLineItem}), per deposit ({@link RefundDeposit}), per surcharge
 * ({@link RefundSurcharge}), and the delivery, overpaid and additional-services
 * amounts; any component left unset is not refunded.
 *
 * <pre>{@code
 * // full refund
 * RefundRequest refund = RefundRequest.builder()
 *         .paymentId(paymentId)
 *         .orderId(orderId)
 *         .commandId(UUID.randomUUID().toString())
 *         .reason(RefundReason.COMPLAINT)
 *         .build();
 *
 * // partial refund: one line item plus the delivery cost
 * RefundRequest partial = RefundRequest.builder()
 *         .paymentId(paymentId)
 *         .orderId(orderId)
 *         .commandId(UUID.randomUUID().toString())
 *         .reason(RefundReason.PRODUCT_NOT_AVAILABLE)
 *         .lineItem(RefundLineItem.byAmount(lineItemId, Money.of("19.99", "PLN")))
 *         .delivery(Money.of("9.90", "PLN"))
 *         .sellerComment("One item out of stock")
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
    private static final String ERR_LINE_ITEM = "lineItem must not be null";
    private static final String ERR_DEPOSIT = "deposit must not be null";
    private static final String ERR_SURCHARGE = "surcharge must not be null";

    private final String paymentId;
    private final String orderId;
    private final String commandId;
    private final RefundReason reason;
    private final List<RefundLineItem> lineItems;
    private final List<RefundDeposit> deposits;
    private final List<RefundSurcharge> surcharges;
    private final @Nullable Money delivery;
    private final @Nullable Money overpaid;
    private final @Nullable Money additionalServices;
    private final @Nullable String sellerComment;

    private RefundRequest(Builder builder) {
        this.paymentId = requireUuid(builder.paymentId, ERR_PAYMENT_ID, ERR_PAYMENT_ID_UUID);
        this.orderId = requireUuid(builder.orderId, ERR_ORDER_ID, ERR_ORDER_ID_UUID);
        this.commandId = requireNonBlank(builder.commandId, ERR_COMMAND_ID);
        if (builder.reason == null) {
            throw new IllegalStateException(ERR_REASON);
        }
        this.reason = builder.reason;
        this.lineItems = List.copyOf(builder.lineItems);
        this.deposits = List.copyOf(builder.deposits);
        this.surcharges = List.copyOf(builder.surcharges);
        this.delivery = builder.delivery;
        this.overpaid = builder.overpaid;
        this.additionalServices = builder.additionalServices;
        this.sellerComment = builder.sellerComment;
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

    /** The line items to refund; empty for a full refund. */
    public List<RefundLineItem> lineItems() {
        return lineItems;
    }

    /** The deposits to refund; empty when none. */
    public List<RefundDeposit> deposits() {
        return deposits;
    }

    /** The surcharges to refund; empty when none. */
    public List<RefundSurcharge> surcharges() {
        return surcharges;
    }

    /** The delivery amount to refund, or {@code null} when not refunding delivery. */
    public @Nullable Money delivery() {
        return delivery;
    }

    /** The overpaid amount to refund, or {@code null} when none. */
    public @Nullable Money overpaid() {
        return overpaid;
    }

    /** The additional-services amount to refund, or {@code null} when none. */
    public @Nullable Money additionalServices() {
        return additionalServices;
    }

    /** The seller's optional justification for the refund, or {@code null}. */
    public @Nullable String sellerComment() {
        return sellerComment;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        Builder builder = new Builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .commandId(commandId)
                .reason(reason)
                .delivery(delivery)
                .overpaid(overpaid)
                .additionalServices(additionalServices)
                .sellerComment(sellerComment);
        lineItems.forEach(builder::lineItem);
        deposits.forEach(builder::deposit);
        surcharges.forEach(builder::surcharge);
        return builder;
    }

    /** Fluent builder for {@link RefundRequest}. */
    public static final class Builder {

        private @Nullable String paymentId;
        private @Nullable String orderId;
        private @Nullable String commandId;
        private @Nullable RefundReason reason;
        private final List<RefundLineItem> lineItems = new ArrayList<>();
        private final List<RefundDeposit> deposits = new ArrayList<>();
        private final List<RefundSurcharge> surcharges = new ArrayList<>();
        private @Nullable Money delivery;
        private @Nullable Money overpaid;
        private @Nullable Money additionalServices;
        private @Nullable String sellerComment;

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

        /** Add a line item to refund (partial refund). */
        public Builder lineItem(RefundLineItem value) {
            this.lineItems.add(RefundValidation.requireNonNull(value, ERR_LINE_ITEM));
            return this;
        }

        /** Add a deposit to refund (partial refund). */
        public Builder deposit(RefundDeposit value) {
            this.deposits.add(RefundValidation.requireNonNull(value, ERR_DEPOSIT));
            return this;
        }

        /** Add a surcharge to refund (partial refund). */
        public Builder surcharge(RefundSurcharge value) {
            this.surcharges.add(RefundValidation.requireNonNull(value, ERR_SURCHARGE));
            return this;
        }

        /** Set the delivery amount to refund (partial refund). */
        public Builder delivery(@Nullable Money value) {
            this.delivery = value;
            return this;
        }

        /** Set the overpaid amount to refund (partial refund). */
        public Builder overpaid(@Nullable Money value) {
            this.overpaid = value;
            return this;
        }

        /** Set the additional-services amount to refund (partial refund). */
        public Builder additionalServices(@Nullable Money value) {
            this.additionalServices = value;
            return this;
        }

        /** Set the seller's optional justification for the refund. */
        public Builder sellerComment(@Nullable String value) {
            this.sellerComment = value;
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
