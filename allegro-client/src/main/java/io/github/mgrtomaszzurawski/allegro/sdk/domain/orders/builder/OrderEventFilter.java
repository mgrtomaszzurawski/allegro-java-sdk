/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEventType;
import java.util.List;

/**
 * Optional filter for streaming the seller's order event log. {@link #all()}
 * streams events of every type; otherwise only the listed {@link OrderEventType}
 * values are returned.
 *
 * @since 0.4.0
 */
public final class OrderEventFilter {

    private final List<OrderEventType> types;

    private OrderEventFilter(Builder builder) {
        this.types = List.copyOf(builder.types);
    }

    /** Event types to match (any of); empty streams all types. */
    public List<OrderEventType> types() {
        return types;
    }

    /** A filter that streams events of every type. */
    public static OrderEventFilter all() {
        return builder().build();
    }

    /** A filter for exactly the given event types. */
    public static OrderEventFilter ofTypes(OrderEventType... values) {
        return builder().types(values).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().types(types);
    }

    /** Fluent builder for {@link OrderEventFilter}. */
    public static final class Builder {

        private List<OrderEventType> types = List.of();

        /** Keep only events of these types. */
        public Builder types(List<OrderEventType> values) {
            this.types = List.copyOf(values);
            return this;
        }

        /** Keep only events of these types. */
        public Builder types(OrderEventType... values) {
            return types(List.of(values));
        }

        /** Build the filter. */
        public OrderEventFilter build() {
            return new OrderEventFilter(this);
        }
    }
}
