/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A parcel tracking number to register against an order via
 * {@code orders().addTrackingNumber(...)}. The carrier id and the waybill are
 * required; without line item ids the waybill covers the whole order.
 *
 * <pre>{@code
 * ShipmentRequest shipment = ShipmentRequest.builder()
 *         .carrierId("DPD")
 *         .waybill("00123456789")
 *         .build();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class ShipmentRequest {

    private static final String ERR_CARRIER_ID = "carrierId is required";
    private static final String ERR_WAYBILL = "waybill is required";

    private final String carrierId;
    private final String waybill;
    private final @Nullable String carrierName;
    private final List<String> lineItemIds;

    private ShipmentRequest(Builder builder) {
        this.carrierId = require(builder.carrierId, ERR_CARRIER_ID);
        this.waybill = require(builder.waybill, ERR_WAYBILL);
        this.carrierName = builder.carrierName;
        this.lineItemIds = List.copyOf(builder.lineItemIds);
    }

    private static String require(@Nullable String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    /** Carrier identifier (from the {@code orders().carriers()} dictionary). */
    public String carrierId() {
        return carrierId;
    }

    /** The carrier's tracking (waybill) number. */
    public String waybill() {
        return waybill;
    }

    /** Optional carrier name for carriers outside the dictionary, or {@code null}. */
    public @Nullable String carrierName() {
        return carrierName;
    }

    /** Line items covered by this waybill; empty means the whole order. */
    public List<String> lineItemIds() {
        return lineItemIds;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder()
                .carrierId(carrierId)
                .waybill(waybill)
                .carrierName(carrierName)
                .lineItemIds(lineItemIds);
    }

    /** Fluent builder for {@link ShipmentRequest}. */
    public static final class Builder {

        private @Nullable String carrierId;
        private @Nullable String waybill;
        private @Nullable String carrierName;
        private List<String> lineItemIds = List.of();

        /** Set the carrier id (required). */
        public Builder carrierId(@Nullable String value) {
            this.carrierId = value;
            return this;
        }

        /** Set the waybill / tracking number (required). */
        public Builder waybill(@Nullable String value) {
            this.waybill = value;
            return this;
        }

        /** Set an explicit carrier name (optional). */
        public Builder carrierName(@Nullable String value) {
            this.carrierName = value;
            return this;
        }

        /** Restrict the waybill to specific line items (optional). */
        public Builder lineItemIds(List<String> values) {
            this.lineItemIds = List.copyOf(values);
            return this;
        }

        /** Restrict the waybill to specific line items (optional). */
        public Builder lineItemIds(String... values) {
            return lineItemIds(List.of(values));
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if the carrier id or waybill is missing
         */
        public ShipmentRequest build() {
            return new ShipmentRequest(this);
        }
    }
}
