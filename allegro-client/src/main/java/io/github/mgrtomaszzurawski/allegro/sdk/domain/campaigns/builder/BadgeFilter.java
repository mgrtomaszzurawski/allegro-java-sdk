/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import org.jspecify.annotations.Nullable;

/**
 * Filters for streaming a seller's active badges. The {@code marketplaceId} is
 * required (the endpoint scopes badges to one marketplace); {@code offerId} is
 * optional.
 *
 * <pre>{@code
 * BadgeFilter polishBadges = BadgeFilter.builder()
 *         .marketplaceId("allegro-pl")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class BadgeFilter {

    private final String marketplaceId;
    private final @Nullable String offerId;

    private BadgeFilter(Builder builder) {
        this.marketplaceId = builder.marketplaceId;
        this.offerId = builder.offerId;
    }

    /** The marketplace whose badges are streamed. */
    public String marketplaceId() {
        return marketplaceId;
    }

    /** Keep only badges on this offer, or {@code null} for all offers. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .marketplaceId(marketplaceId)
                .offerId(offerId);
    }

    /** Fluent builder for {@link BadgeFilter}; validates the required marketplace fail-fast. */
    public static final class Builder {

        private static final String ERR_MARKETPLACE_REQUIRED = "marketplaceId is required";

        private @Nullable String marketplaceId;
        private @Nullable String offerId;

        private Builder() {
        }

        /** Set the marketplace whose badges are streamed (required). */
        public Builder marketplaceId(@Nullable String badgeMarketplaceId) {
            this.marketplaceId = badgeMarketplaceId;
            return this;
        }

        /** Keep only badges on this offer (optional). */
        public Builder offerId(@Nullable String badgeOfferId) {
            this.offerId = badgeOfferId;
            return this;
        }

        /**
         * Validate and build the filter.
         *
         * @return the immutable filter
         * @throws IllegalStateException if {@code marketplaceId} is missing or blank
         */
        public BadgeFilter build() {
            if (marketplaceId == null || marketplaceId.isBlank()) {
                throw new IllegalStateException(ERR_MARKETPLACE_REQUIRED);
            }
            return new BadgeFilter(this);
        }
    }
}
