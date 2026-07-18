/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional filter for streaming Advance Ship Notices
 * ({@code advanceShipNotices().streamNotices(filter)}). Restrict the stream to
 * one or more lifecycle {@link AsnStatus statuses}; {@link #all()} streams every
 * notice.
 *
 * <pre>{@code
 * AsnFilter drafts = AsnFilter.builder()
 *         .addStatus(AsnStatus.DRAFT)
 *         .addStatus(AsnStatus.IN_TRANSIT)
 *         .build();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class AsnFilter {

    private final List<AsnStatus> statuses;

    private AsnFilter(Builder builder) {
        this.statuses = List.copyOf(builder.statuses);
    }

    /** The statuses to restrict the stream to (never {@code null}; empty means no restriction). */
    public List<AsnStatus> statuses() {
        return statuses;
    }

    /** A filter that streams every notice. */
    public static AsnFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().statuses(statuses);
    }

    /** Fluent builder for {@link AsnFilter}. */
    public static final class Builder {

        private final List<AsnStatus> statuses = new ArrayList<>();

        /** Add one status to the filter. */
        public Builder addStatus(AsnStatus status) {
            this.statuses.add(status);
            return this;
        }

        /** Replace the filter's statuses with the given collection. */
        public Builder statuses(List<AsnStatus> statuses) {
            this.statuses.clear();
            this.statuses.addAll(statuses);
            return this;
        }

        /** Build the filter. */
        public AsnFilter build() {
            return new AsnFilter(this);
        }
    }
}
