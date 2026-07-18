/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional date-time bounds for the classifieds statistics reads
 * ({@code Classifieds.offerStats(...)} / {@code sellerStats(...)}). Both bounds
 * are optional; {@link #all()} applies the server's default range. Allegro
 * requires the two bounds to be less than three months apart and both earlier
 * than now — it rejects a wider range with a bad-request error.
 *
 * <pre>{@code
 * ClassifiedStatsFilter lastMonth = ClassifiedStatsFilter.builder()
 *         .eventsFrom(OffsetDateTime.now().minusMonths(1))
 *         .eventsTo(OffsetDateTime.now())
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ClassifiedStatsFilter {

    private final @Nullable OffsetDateTime eventsFrom;
    private final @Nullable OffsetDateTime eventsTo;

    private ClassifiedStatsFilter(Builder builder) {
        this.eventsFrom = builder.eventsFrom;
        this.eventsTo = builder.eventsTo;
    }

    /** Lower bound (inclusive) on the events, mapped to {@code date.gte}, or {@code null}. */
    public @Nullable OffsetDateTime eventsFrom() {
        return eventsFrom;
    }

    /** Upper bound (inclusive) on the events, mapped to {@code date.lte}, or {@code null}. */
    public @Nullable OffsetDateTime eventsTo() {
        return eventsTo;
    }

    /** A filter that leaves the range to the server default. */
    public static ClassifiedStatsFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().eventsFrom(eventsFrom).eventsTo(eventsTo);
    }

    /** Fluent builder for {@link ClassifiedStatsFilter}. */
    public static final class Builder {

        private @Nullable OffsetDateTime eventsFrom;
        private @Nullable OffsetDateTime eventsTo;

        /** Count events at or after this instant ({@code date.gte}). */
        public Builder eventsFrom(@Nullable OffsetDateTime lowerBound) {
            this.eventsFrom = lowerBound;
            return this;
        }

        /** Count events at or before this instant ({@code date.lte}). */
        public Builder eventsTo(@Nullable OffsetDateTime upperBound) {
            this.eventsTo = upperBound;
            return this;
        }

        /** Build the filter. */
        public ClassifiedStatsFilter build() {
            return new ClassifiedStatsFilter(this);
        }
    }
}
