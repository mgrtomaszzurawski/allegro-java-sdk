/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import org.jspecify.annotations.Nullable;

/**
 * A request to claim a commission refund for one order line item, passed to
 * {@code orders().commissionRefunds().claim(...)}. The line item id and a
 * positive quantity are required.
 *
 * @since 0.6.0
 */
public final class RefundClaimRequest {

    private static final String ERR_LINE_ITEM = "lineItemId is required";
    private static final String ERR_QUANTITY = "quantity must be positive";

    private final String lineItemId;
    private final int quantity;

    private RefundClaimRequest(Builder builder) {
        if (builder.lineItemId == null || builder.lineItemId.isBlank()) {
            throw new IllegalStateException(ERR_LINE_ITEM);
        }
        if (builder.quantity <= 0) {
            throw new IllegalStateException(ERR_QUANTITY);
        }
        this.lineItemId = builder.lineItemId;
        this.quantity = builder.quantity;
    }

    /** The line item the commission refund is claimed for. */
    public String lineItemId() {
        return lineItemId;
    }

    /** The claimed quantity (positive). */
    public int quantity() {
        return quantity;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder().lineItemId(lineItemId).quantity(quantity);
    }

    /** Fluent builder for {@link RefundClaimRequest}. */
    public static final class Builder {

        private @Nullable String lineItemId;
        private int quantity;

        /** Set the line item id (required). */
        public Builder lineItemId(@Nullable String value) {
            this.lineItemId = value;
            return this;
        }

        /** Set the claimed quantity (required, positive). */
        public Builder quantity(int value) {
            this.quantity = value;
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if the line item id is missing or the
         *     quantity is not positive
         */
        public RefundClaimRequest build() {
            return new RefundClaimRequest(this);
        }
    }
}
