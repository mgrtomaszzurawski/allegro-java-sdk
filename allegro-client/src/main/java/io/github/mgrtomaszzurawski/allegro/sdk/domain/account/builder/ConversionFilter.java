/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.ConversionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming affiliate CPS conversions. All fields are
 * optional; {@link #all()} matches every conversion.
 *
 * <pre>{@code
 * ConversionFilter confirmed = ConversionFilter.builder()
 *         .status(ConversionStatus.CONFIRMED)
 *         .orderCreatedFrom(OffsetDateTime.now().minusMonths(1))
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ConversionFilter {

    private final @Nullable OffsetDateTime orderCreatedFrom;
    private final @Nullable OffsetDateTime orderCreatedTo;
    private final @Nullable OffsetDateTime lastModifiedFrom;
    private final @Nullable OffsetDateTime lastModifiedTo;
    private final @Nullable ConversionStatus status;
    private final List<String> includePublisherUrlParameterKeys;

    private ConversionFilter(Builder builder) {
        this.orderCreatedFrom = builder.orderCreatedFrom;
        this.orderCreatedTo = builder.orderCreatedTo;
        this.lastModifiedFrom = builder.lastModifiedFrom;
        this.lastModifiedTo = builder.lastModifiedTo;
        this.status = builder.status;
        this.includePublisherUrlParameterKeys = List.copyOf(builder.includePublisherUrlParameterKeys);
    }

    /** Lower bound (inclusive) on order creation time, or {@code null}. */
    public @Nullable OffsetDateTime orderCreatedFrom() {
        return orderCreatedFrom;
    }

    /** Upper bound (inclusive) on order creation time, or {@code null}. */
    public @Nullable OffsetDateTime orderCreatedTo() {
        return orderCreatedTo;
    }

    /** Lower bound (inclusive) on last-modification time, or {@code null}. */
    public @Nullable OffsetDateTime lastModifiedFrom() {
        return lastModifiedFrom;
    }

    /** Upper bound (inclusive) on last-modification time, or {@code null}. */
    public @Nullable OffsetDateTime lastModifiedTo() {
        return lastModifiedTo;
    }

    /** Filter by conversion status, or {@code null} for any. */
    public @Nullable ConversionStatus status() {
        return status;
    }

    /** Publisher URL parameter keys to include in the response; never {@code null}. */
    public List<String> includePublisherUrlParameterKeys() {
        return includePublisherUrlParameterKeys;
    }

    /** A filter that matches every conversion. */
    public static ConversionFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .orderCreatedFrom(orderCreatedFrom)
                .orderCreatedTo(orderCreatedTo)
                .lastModifiedFrom(lastModifiedFrom)
                .lastModifiedTo(lastModifiedTo)
                .status(status)
                .includePublisherUrlParameterKeys(includePublisherUrlParameterKeys);
    }

    /** Fluent builder for {@link ConversionFilter}. */
    public static final class Builder {

        private @Nullable OffsetDateTime orderCreatedFrom;
        private @Nullable OffsetDateTime orderCreatedTo;
        private @Nullable OffsetDateTime lastModifiedFrom;
        private @Nullable OffsetDateTime lastModifiedTo;
        private @Nullable ConversionStatus status;
        private List<String> includePublisherUrlParameterKeys = List.of();

        /** Keep conversions whose order was created at or after this instant. */
        public Builder orderCreatedFrom(@Nullable OffsetDateTime orderCreatedFrom) {
            this.orderCreatedFrom = orderCreatedFrom;
            return this;
        }

        /** Keep conversions whose order was created at or before this instant. */
        public Builder orderCreatedTo(@Nullable OffsetDateTime orderCreatedTo) {
            this.orderCreatedTo = orderCreatedTo;
            return this;
        }

        /** Keep conversions last modified at or after this instant. */
        public Builder lastModifiedFrom(@Nullable OffsetDateTime lastModifiedFrom) {
            this.lastModifiedFrom = lastModifiedFrom;
            return this;
        }

        /** Keep conversions last modified at or before this instant. */
        public Builder lastModifiedTo(@Nullable OffsetDateTime lastModifiedTo) {
            this.lastModifiedTo = lastModifiedTo;
            return this;
        }

        /** Keep only conversions with this status. */
        public Builder status(@Nullable ConversionStatus status) {
            this.status = status;
            return this;
        }

        /** Publisher URL parameter keys to include in the response. */
        public Builder includePublisherUrlParameterKeys(List<String> keys) {
            this.includePublisherUrlParameterKeys = List.copyOf(keys);
            return this;
        }

        /** Build the filter. */
        public ConversionFilter build() {
            return new ConversionFilter(this);
        }
    }
}
