/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import org.jspecify.annotations.Nullable;

/**
 * Optional filters for streaming a seller's badge applications. All fields are
 * optional; {@link #all()} streams every application.
 *
 * <pre>{@code
 * BadgeApplicationFilter forOffer = BadgeApplicationFilter.builder()
 *         .offerId("12345678")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class BadgeApplicationFilter {

    private final @Nullable String campaignId;
    private final @Nullable String offerId;

    private BadgeApplicationFilter(Builder builder) {
        this.campaignId = builder.campaignId;
        this.offerId = builder.offerId;
    }

    /** Keep only applications for this campaign, or {@code null} for all campaigns. */
    public @Nullable String campaignId() {
        return campaignId;
    }

    /** Keep only applications for this offer, or {@code null} for all offers. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** A filter that matches every application. */
    public static BadgeApplicationFilter all() {
        return builder().build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder()
                .campaignId(campaignId)
                .offerId(offerId);
    }

    /** Fluent builder for {@link BadgeApplicationFilter}. */
    public static final class Builder {

        private @Nullable String campaignId;
        private @Nullable String offerId;

        private Builder() {
        }

        /** Keep only applications for this campaign. */
        public Builder campaignId(@Nullable String badgeCampaignId) {
            this.campaignId = badgeCampaignId;
            return this;
        }

        /** Keep only applications for this offer. */
        public Builder offerId(@Nullable String badgeOfferId) {
            this.offerId = badgeOfferId;
            return this;
        }

        /** Build the immutable filter. */
        public BadgeApplicationFilter build() {
            return new BadgeApplicationFilter(this);
        }
    }
}
