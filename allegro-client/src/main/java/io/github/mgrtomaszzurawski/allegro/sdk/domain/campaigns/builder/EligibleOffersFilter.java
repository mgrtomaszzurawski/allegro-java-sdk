/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import org.jspecify.annotations.Nullable;

/**
 * The filter for {@code alleDiscount().streamEligibleOffers(...)}. The
 * {@code campaignId} is required (it scopes the request path); the offer id and
 * the {@code meetsConditions} flag are optional.
 *
 * <pre>{@code
 * EligibleOffersFilter filter = EligibleOffersFilter.builder("winter-sale")
 *         .meetsConditions(true)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class EligibleOffersFilter {

    private final String campaignId;
    private final @Nullable String offerId;
    private final @Nullable Boolean meetsConditions;

    private EligibleOffersFilter(Builder builder) {
        this.campaignId = builder.campaignId;
        this.offerId = builder.offerId;
        this.meetsConditions = builder.meetsConditions;
    }

    /** The campaign whose eligible offers are streamed. */
    public String campaignId() {
        return campaignId;
    }

    /** Keep only this offer, or {@code null} for all eligible offers. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** Keep only offers that meet ({@code true}) or fail ({@code false}) the conditions, or {@code null}. */
    public @Nullable Boolean meetsConditions() {
        return meetsConditions;
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
        return new Builder(campaignId).offerId(offerId).meetsConditions(meetsConditions);
    }

    /** Fluent builder for {@link EligibleOffersFilter}; validates the campaign fail-fast. */
    public static final class Builder {

        private static final String ERR_CAMPAIGN_REQUIRED = "campaignId is required";

        private final String campaignId;
        private @Nullable String offerId;
        private @Nullable Boolean meetsConditions;

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

        /** Keep only offers that meet or fail the campaign conditions. */
        public Builder meetsConditions(@Nullable Boolean meets) {
            this.meetsConditions = meets;
            return this;
        }

        /** Build the immutable filter. */
        public EligibleOffersFilter build() {
            return new EligibleOffersFilter(this);
        }
    }
}
