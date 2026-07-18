/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import org.jspecify.annotations.Nullable;

/**
 * The filter for {@code alleDiscount().streamSubmittedOffers(...)}. The
 * {@code campaignId} is required (it scopes the request path); the offer id and
 * participation id are optional.
 *
 * <pre>{@code
 * SubmittedOffersFilter filter = SubmittedOffersFilter.builder("winter-sale")
 *         .offerId("12345678")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class SubmittedOffersFilter {

    private final String campaignId;
    private final @Nullable String offerId;
    private final @Nullable String participationId;

    private SubmittedOffersFilter(Builder builder) {
        this.campaignId = builder.campaignId;
        this.offerId = builder.offerId;
        this.participationId = builder.participationId;
    }

    /** The campaign whose submitted offers are streamed. */
    public String campaignId() {
        return campaignId;
    }

    /** Keep only this offer, or {@code null} for all submitted offers. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** Keep only this participation, or {@code null} for all. */
    public @Nullable String participationId() {
        return participationId;
    }

    /**
     * A new builder for the given campaign (required).
     *
     * @param campaignId the campaign id; must not be null or blank
     * @return a new builder
     */
    public static Builder builder(String campaignId) {
        return new Builder(campaignId);
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder(campaignId).offerId(offerId).participationId(participationId);
    }

    /** Fluent builder for {@link SubmittedOffersFilter}; validates the campaign fail-fast. */
    public static final class Builder {

        private static final String ERR_CAMPAIGN_REQUIRED = "campaignId is required";

        private final String campaignId;
        private @Nullable String offerId;
        private @Nullable String participationId;

        private Builder(String campaignId) {
            if (campaignId == null || campaignId.isBlank()) {
                throw new IllegalArgumentException(ERR_CAMPAIGN_REQUIRED);
            }
            this.campaignId = campaignId;
        }

        /** Keep only this offer. */
        public Builder offerId(@Nullable String alleDiscountOfferId) {
            this.offerId = alleDiscountOfferId;
            return this;
        }

        /** Keep only this participation. */
        public Builder participationId(@Nullable String alleDiscountParticipationId) {
            this.participationId = alleDiscountParticipationId;
            return this;
        }

        /** Build the immutable filter. */
        public SubmittedOffersFilter build() {
            return new SubmittedOffersFilter(this);
        }
    }
}
