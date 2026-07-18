/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * The serial numbers to assign to an order's line items via
 * {@code orders().setSerialNumbers(...)}. At least one line item must be added,
 * each with at least one serial number.
 *
 * <pre>{@code
 * SerialNumbersRequest serials = SerialNumbersRequest.builder()
 *         .lineItem("0f3e...-lineItemId", "SN-001", "SN-002")
 *         .build();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class SerialNumbersRequest {

    private static final String ERR_NO_LINE_ITEMS = "at least one line item is required";
    private static final String ERR_BLANK_LINE_ITEM = "lineItemId is required";
    private static final String ERR_NO_SERIALS = "at least one serial number is required per line item";

    private final List<LineItemSerialNumbers> lineItems;

    private SerialNumbersRequest(Builder builder) {
        if (builder.lineItems.isEmpty()) {
            throw new IllegalStateException(ERR_NO_LINE_ITEMS);
        }
        this.lineItems = List.copyOf(builder.lineItems);
    }

    /** The per-line-item serial number assignments; never empty. */
    public List<LineItemSerialNumbers> lineItems() {
        return lineItems;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        for (LineItemSerialNumbers entry : lineItems) {
            builder.lineItem(entry.lineItemId(), entry.serialNumbers());
        }
        return builder;
    }

    /**
     * Serial numbers assigned to one line item.
     *
     * @param lineItemId the target line item's identifier
     * @param serialNumbers the serial numbers, in order; never empty
     */
    public record LineItemSerialNumbers(String lineItemId, List<String> serialNumbers) {

        public LineItemSerialNumbers {
            serialNumbers = List.copyOf(serialNumbers);
        }
    }

    /** Fluent builder for {@link SerialNumbersRequest}. */
    public static final class Builder {

        private final List<LineItemSerialNumbers> lineItems = new ArrayList<>();

        /** Assign serial numbers to one line item. */
        public Builder lineItem(String lineItemId, List<String> serialNumbers) {
            if (lineItemId.isBlank()) {
                throw new IllegalStateException(ERR_BLANK_LINE_ITEM);
            }
            if (serialNumbers.isEmpty()) {
                throw new IllegalStateException(ERR_NO_SERIALS);
            }
            lineItems.add(new LineItemSerialNumbers(lineItemId, serialNumbers));
            return this;
        }

        /** Assign serial numbers to one line item. */
        public Builder lineItem(String lineItemId, String... serialNumbers) {
            return lineItem(lineItemId, List.of(serialNumbers));
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if no line item was added
         */
        public SerialNumbersRequest build() {
            return new SerialNumbersRequest(this);
        }
    }
}
