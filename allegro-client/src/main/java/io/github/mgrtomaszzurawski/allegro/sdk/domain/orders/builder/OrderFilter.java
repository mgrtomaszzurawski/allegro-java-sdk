/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming the seller's orders. Every field is optional;
 * {@link #all()} streams every order. Repeated-value fields (statuses) match any
 * of the listed values.
 *
 * <pre>{@code
 * OrderFilter toShip = OrderFilter.builder()
 *         .fulfillmentStatuses(SellerStatus.READY_FOR_SHIPMENT)
 *         .updatedFrom(OffsetDateTime.now().minusDays(7))
 *         .build();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class OrderFilter {

    private final List<OrderStatus> statuses;
    private final List<SellerStatus> fulfillmentStatuses;
    private final @Nullable String fulfillmentProviderId;
    private final @Nullable Boolean lineItemsSent;
    private final @Nullable OffsetDateTime boughtFrom;
    private final @Nullable OffsetDateTime boughtTo;
    private final @Nullable OffsetDateTime updatedFrom;
    private final @Nullable OffsetDateTime updatedTo;
    private final @Nullable String buyerLogin;
    private final @Nullable String marketplaceId;
    private final @Nullable String paymentId;
    private final @Nullable String surchargeId;
    private final @Nullable String deliveryMethodId;

    private OrderFilter(Builder builder) {
        this.statuses = List.copyOf(builder.statuses);
        this.fulfillmentStatuses = List.copyOf(builder.fulfillmentStatuses);
        this.fulfillmentProviderId = builder.fulfillmentProviderId;
        this.lineItemsSent = builder.lineItemsSent;
        this.boughtFrom = builder.boughtFrom;
        this.boughtTo = builder.boughtTo;
        this.updatedFrom = builder.updatedFrom;
        this.updatedTo = builder.updatedTo;
        this.buyerLogin = builder.buyerLogin;
        this.marketplaceId = builder.marketplaceId;
        this.paymentId = builder.paymentId;
        this.surchargeId = builder.surchargeId;
        this.deliveryMethodId = builder.deliveryMethodId;
    }

    /** Buyer-side statuses to match (any of); empty matches all. */
    public List<OrderStatus> statuses() {
        return statuses;
    }

    /** Seller-side fulfillment statuses to match (any of); empty matches all. */
    public List<SellerStatus> fulfillmentStatuses() {
        return fulfillmentStatuses;
    }

    /** Fulfillment provider id to match, or {@code null}. */
    public @Nullable String fulfillmentProviderId() {
        return fulfillmentProviderId;
    }

    /** Whether all line items have been marked sent, or {@code null} for both. */
    public @Nullable Boolean lineItemsSent() {
        return lineItemsSent;
    }

    /** Lower bound (inclusive) on line-item purchase time, or {@code null}. */
    public @Nullable OffsetDateTime boughtFrom() {
        return boughtFrom;
    }

    /** Upper bound (inclusive) on line-item purchase time, or {@code null}. */
    public @Nullable OffsetDateTime boughtTo() {
        return boughtTo;
    }

    /** Lower bound (inclusive) on last-update time, or {@code null}. */
    public @Nullable OffsetDateTime updatedFrom() {
        return updatedFrom;
    }

    /** Upper bound (inclusive) on last-update time, or {@code null}. */
    public @Nullable OffsetDateTime updatedTo() {
        return updatedTo;
    }

    /** Buyer login to match, or {@code null}. */
    public @Nullable String buyerLogin() {
        return buyerLogin;
    }

    /** Marketplace id to match, or {@code null}. */
    public @Nullable String marketplaceId() {
        return marketplaceId;
    }

    /** Payment id to match, or {@code null}. */
    public @Nullable String paymentId() {
        return paymentId;
    }

    /** Surcharge id to match, or {@code null}. */
    public @Nullable String surchargeId() {
        return surchargeId;
    }

    /** Delivery method id to match, or {@code null}. */
    public @Nullable String deliveryMethodId() {
        return deliveryMethodId;
    }

    /** A filter that streams every order. */
    public static OrderFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .statuses(statuses)
                .fulfillmentStatuses(fulfillmentStatuses)
                .fulfillmentProviderId(fulfillmentProviderId)
                .lineItemsSent(lineItemsSent)
                .boughtFrom(boughtFrom)
                .boughtTo(boughtTo)
                .updatedFrom(updatedFrom)
                .updatedTo(updatedTo)
                .buyerLogin(buyerLogin)
                .marketplaceId(marketplaceId)
                .paymentId(paymentId)
                .surchargeId(surchargeId)
                .deliveryMethodId(deliveryMethodId);
    }

    /** Fluent builder for {@link OrderFilter}. */
    public static final class Builder {

        private List<OrderStatus> statuses = List.of();
        private List<SellerStatus> fulfillmentStatuses = List.of();
        private @Nullable String fulfillmentProviderId;
        private @Nullable Boolean lineItemsSent;
        private @Nullable OffsetDateTime boughtFrom;
        private @Nullable OffsetDateTime boughtTo;
        private @Nullable OffsetDateTime updatedFrom;
        private @Nullable OffsetDateTime updatedTo;
        private @Nullable String buyerLogin;
        private @Nullable String marketplaceId;
        private @Nullable String paymentId;
        private @Nullable String surchargeId;
        private @Nullable String deliveryMethodId;

        /** Keep orders whose buyer-side status is any of these. */
        public Builder statuses(List<OrderStatus> values) {
            this.statuses = List.copyOf(values);
            return this;
        }

        /** Keep orders whose buyer-side status is any of these. */
        public Builder statuses(OrderStatus... values) {
            return statuses(List.of(values));
        }

        /** Keep orders whose seller-side fulfillment status is any of these. */
        public Builder fulfillmentStatuses(List<SellerStatus> values) {
            this.fulfillmentStatuses = List.copyOf(values);
            return this;
        }

        /** Keep orders whose seller-side fulfillment status is any of these. */
        public Builder fulfillmentStatuses(SellerStatus... values) {
            return fulfillmentStatuses(List.of(values));
        }

        /** Keep orders handled by this fulfillment provider. */
        public Builder fulfillmentProviderId(@Nullable String value) {
            this.fulfillmentProviderId = value;
            return this;
        }

        /** Keep orders whose line items are all sent ({@code true}) or not ({@code false}). */
        public Builder lineItemsSent(@Nullable Boolean value) {
            this.lineItemsSent = value;
            return this;
        }

        /** Keep orders with a line item bought at or after this instant. */
        public Builder boughtFrom(@Nullable OffsetDateTime value) {
            this.boughtFrom = value;
            return this;
        }

        /** Keep orders with a line item bought at or before this instant. */
        public Builder boughtTo(@Nullable OffsetDateTime value) {
            this.boughtTo = value;
            return this;
        }

        /** Keep orders last updated at or after this instant. */
        public Builder updatedFrom(@Nullable OffsetDateTime value) {
            this.updatedFrom = value;
            return this;
        }

        /** Keep orders last updated at or before this instant. */
        public Builder updatedTo(@Nullable OffsetDateTime value) {
            this.updatedTo = value;
            return this;
        }

        /** Keep orders placed by this buyer login. */
        public Builder buyerLogin(@Nullable String value) {
            this.buyerLogin = value;
            return this;
        }

        /** Keep orders placed on this marketplace. */
        public Builder marketplaceId(@Nullable String value) {
            this.marketplaceId = value;
            return this;
        }

        /** Keep orders paid via this payment id. */
        public Builder paymentId(@Nullable String value) {
            this.paymentId = value;
            return this;
        }

        /** Keep orders carrying this surcharge id. */
        public Builder surchargeId(@Nullable String value) {
            this.surchargeId = value;
            return this;
        }

        /** Keep orders delivered via this delivery method id. */
        public Builder deliveryMethodId(@Nullable String value) {
            this.deliveryMethodId = value;
            return this;
        }

        /** Build the filter. */
        public OrderFilter build() {
            return new OrderFilter(this);
        }
    }
}
