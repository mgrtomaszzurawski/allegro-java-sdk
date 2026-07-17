/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming the seller's received ratings. All fields are
 * optional; {@link #all()} matches every rating.
 *
 * <pre>{@code
 * RatingFilter recent = RatingFilter.builder()
 *         .recommended(false)
 *         .changedFrom(OffsetDateTime.now().minusDays(30))
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class RatingFilter {

    private final @Nullable Boolean recommended;
    private final @Nullable OffsetDateTime changedFrom;
    private final @Nullable OffsetDateTime changedTo;

    private RatingFilter(Builder builder) {
        this.recommended = builder.recommended;
        this.changedFrom = builder.changedFrom;
        this.changedTo = builder.changedTo;
    }

    /** Filter by whether the buyer recommended the purchase, or {@code null} for both. */
    public @Nullable Boolean recommended() {
        return recommended;
    }

    /** Lower bound (inclusive) on the rating's last-change time, or {@code null}. */
    public @Nullable OffsetDateTime changedFrom() {
        return changedFrom;
    }

    /** Upper bound (inclusive) on the rating's last-change time, or {@code null}. */
    public @Nullable OffsetDateTime changedTo() {
        return changedTo;
    }

    /** A filter that matches every rating. */
    public static RatingFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .recommended(recommended)
                .changedFrom(changedFrom)
                .changedTo(changedTo);
    }

    /** Fluent builder for {@link RatingFilter}. */
    public static final class Builder {

        private @Nullable Boolean recommended;
        private @Nullable OffsetDateTime changedFrom;
        private @Nullable OffsetDateTime changedTo;

        /** Keep only recommended ({@code true}) or not-recommended ({@code false}) ratings. */
        public Builder recommended(@Nullable Boolean recommended) {
            this.recommended = recommended;
            return this;
        }

        /** Keep ratings last changed at or after this instant. */
        public Builder changedFrom(@Nullable OffsetDateTime changedFrom) {
            this.changedFrom = changedFrom;
            return this;
        }

        /** Keep ratings last changed at or before this instant. */
        public Builder changedTo(@Nullable OffsetDateTime changedTo) {
            this.changedTo = changedTo;
            return this;
        }

        /** Build the filter. */
        public RatingFilter build() {
            return new RatingFilter(this);
        }
    }
}
