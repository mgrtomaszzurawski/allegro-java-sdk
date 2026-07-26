/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ScheduledChangeType;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Filter for the scheduled category-parameter-change stream
 * ({@code catalog().categories().scheduledParameterChanges(...)}).
 *
 * <p>All criteria are optional — {@link #all()} streams every planned change. Bound
 * by when a change <em>takes effect</em>
 * ({@link Builder#scheduledForFrom(OffsetDateTime) scheduledForFrom} /
 * {@link Builder#scheduledForTo(OffsetDateTime) scheduledForTo}), when it was
 * <em>announced</em> ({@link Builder#scheduledAtFrom(OffsetDateTime) scheduledAtFrom} /
 * {@link Builder#scheduledAtTo(OffsetDateTime) scheduledAtTo}), and restrict to
 * certain change {@link Builder#types(ScheduledChangeType...) types}.
 *
 * @since 0.2.0
 */
public final class CategoryParameterChangeFilter {

    private final @Nullable OffsetDateTime scheduledForFrom;
    private final @Nullable OffsetDateTime scheduledForTo;
    private final @Nullable OffsetDateTime scheduledAtFrom;
    private final @Nullable OffsetDateTime scheduledAtTo;
    private final List<ScheduledChangeType> types;

    private CategoryParameterChangeFilter(Builder builder) {
        this.scheduledForFrom = builder.scheduledForFrom;
        this.scheduledForTo = builder.scheduledForTo;
        this.scheduledAtFrom = builder.scheduledAtFrom;
        this.scheduledAtTo = builder.scheduledAtTo;
        this.types = List.copyOf(builder.types);
    }

    /** Lower bound (inclusive) on when a change takes effect, or {@code null}. */
    public @Nullable OffsetDateTime scheduledForFrom() {
        return scheduledForFrom;
    }

    /** Upper bound (inclusive) on when a change takes effect, or {@code null}. */
    public @Nullable OffsetDateTime scheduledForTo() {
        return scheduledForTo;
    }

    /** Lower bound (inclusive) on when a change was announced, or {@code null}. */
    public @Nullable OffsetDateTime scheduledAtFrom() {
        return scheduledAtFrom;
    }

    /** Upper bound (inclusive) on when a change was announced, or {@code null}. */
    public @Nullable OffsetDateTime scheduledAtTo() {
        return scheduledAtTo;
    }

    /** The change kinds to include; empty for all. */
    public List<ScheduledChangeType> types() {
        return types;
    }

    /** Every planned change (no bounds). */
    public static CategoryParameterChangeFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link CategoryParameterChangeFilter}. */
    public static final class Builder {

        private @Nullable OffsetDateTime scheduledForFrom;
        private @Nullable OffsetDateTime scheduledForTo;
        private @Nullable OffsetDateTime scheduledAtFrom;
        private @Nullable OffsetDateTime scheduledAtTo;
        private List<ScheduledChangeType> types = List.of();

        /** Only changes taking effect at or after this instant. */
        public Builder scheduledForFrom(@Nullable OffsetDateTime scheduledForFrom) {
            this.scheduledForFrom = scheduledForFrom;
            return this;
        }

        /** Only changes taking effect at or before this instant. */
        public Builder scheduledForTo(@Nullable OffsetDateTime scheduledForTo) {
            this.scheduledForTo = scheduledForTo;
            return this;
        }

        /** Only changes announced at or after this instant. */
        public Builder scheduledAtFrom(@Nullable OffsetDateTime scheduledAtFrom) {
            this.scheduledAtFrom = scheduledAtFrom;
            return this;
        }

        /** Only changes announced at or before this instant. */
        public Builder scheduledAtTo(@Nullable OffsetDateTime scheduledAtTo) {
            this.scheduledAtTo = scheduledAtTo;
            return this;
        }

        /** Restrict to these change kinds (empty for all). */
        public Builder types(ScheduledChangeType... types) {
            this.types = List.of(types);
            return this;
        }

        /** Build the filter. */
        public CategoryParameterChangeFilter build() {
            return new CategoryParameterChangeFilter(this);
        }
    }
}
