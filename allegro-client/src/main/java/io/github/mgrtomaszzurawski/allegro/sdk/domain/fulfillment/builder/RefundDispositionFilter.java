/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional creation-time bounds for the refund-dispositions report
 * ({@code fulfillment().refundDispositions(filter)}). Both bounds are optional;
 * {@link #all()} returns every disposition.
 *
 * <pre>{@code
 * RefundDispositionFilter lastWeek = RefundDispositionFilter.builder()
 *         .createdFrom(OffsetDateTime.now().minusDays(7))
 *         .build();
 * }</pre>
 *
 * @since 0.3.0
 */
public final class RefundDispositionFilter {

    private final @Nullable OffsetDateTime createdFrom;
    private final @Nullable OffsetDateTime createdTo;

    private RefundDispositionFilter(Builder builder) {
        this.createdFrom = builder.createdFrom;
        this.createdTo = builder.createdTo;
    }

    /** Lower bound (inclusive) on the disposition's creation time, or {@code null}. */
    public @Nullable OffsetDateTime createdFrom() {
        return createdFrom;
    }

    /** Upper bound (inclusive) on the disposition's creation time, or {@code null}. */
    public @Nullable OffsetDateTime createdTo() {
        return createdTo;
    }

    /** A filter that returns every disposition. */
    public static RefundDispositionFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .createdFrom(createdFrom)
                .createdTo(createdTo);
    }

    /** Fluent builder for {@link RefundDispositionFilter}. */
    public static final class Builder {

        private @Nullable OffsetDateTime createdFrom;
        private @Nullable OffsetDateTime createdTo;

        /** Keep dispositions created at or after this instant. */
        public Builder createdFrom(@Nullable OffsetDateTime createdFrom) {
            this.createdFrom = createdFrom;
            return this;
        }

        /** Keep dispositions created at or before this instant. */
        public Builder createdTo(@Nullable OffsetDateTime createdTo) {
            this.createdTo = createdTo;
            return this;
        }

        /** Build the filter. */
        public RefundDispositionFilter build() {
            return new RefundDispositionFilter(this);
        }
    }
}
