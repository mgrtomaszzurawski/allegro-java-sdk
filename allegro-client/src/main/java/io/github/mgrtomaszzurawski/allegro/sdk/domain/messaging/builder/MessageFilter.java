/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Optional time-window filter for streaming the messages of a thread. Both
 * bounds are optional; an empty filter streams the whole thread.
 *
 * <pre>{@code
 * MessageFilter recent = MessageFilter.builder()
 *         .after(OffsetDateTime.now().minusDays(7))
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class MessageFilter {

    private final @Nullable OffsetDateTime before;
    private final @Nullable OffsetDateTime after;

    private MessageFilter(Builder builder) {
        this.before = builder.before;
        this.after = builder.after;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** An empty filter (no bounds). */
    public static MessageFilter none() {
        return new Builder().build();
    }

    /** Upper bound: only messages created before this instant, or {@code null}. */
    public @Nullable OffsetDateTime before() {
        return before;
    }

    /** Lower bound: only messages created after this instant, or {@code null}. */
    public @Nullable OffsetDateTime after() {
        return after;
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.before = before;
        builder.after = after;
        return builder;
    }

    /** Fluent builder for {@link MessageFilter}. */
    public static final class Builder {

        private @Nullable OffsetDateTime before;
        private @Nullable OffsetDateTime after;

        private Builder() {
        }

        /** Restrict to messages created strictly before {@code upperBound}. */
        public Builder before(OffsetDateTime upperBound) {
            this.before = upperBound;
            return this;
        }

        /** Restrict to messages created strictly after {@code lowerBound}. */
        public Builder after(OffsetDateTime lowerBound) {
            this.after = lowerBound;
            return this;
        }

        /** Build the immutable filter. */
        public MessageFilter build() {
            return new MessageFilter(this);
        }
    }
}
