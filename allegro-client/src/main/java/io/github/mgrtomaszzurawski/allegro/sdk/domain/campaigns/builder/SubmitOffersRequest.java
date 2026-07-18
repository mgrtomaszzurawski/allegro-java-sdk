/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The offers to submit to Allegro Prices, passed to
 * {@code allegroPrices().submitOffers(...)}. Between 1 and 1000 offers per command;
 * each offer optionally carries the seller's maximum subsidy contribution.
 *
 * <pre>{@code
 * SubmitOffersRequest request = SubmitOffersRequest.builder()
 *         .addOffer("12345678", "allegro-pl", "5")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class SubmitOffersRequest {

    /**
     * One offer to submit.
     *
     * @param offerId                   the offer
     * @param marketplaceId             the marketplace to submit it on
     * @param maxContributionPercentage the seller's max subsidy contribution %, or {@code null}
     */
    public record Offer(String offerId, String marketplaceId,
            @Nullable String maxContributionPercentage) {
    }

    private final List<Offer> offers;

    private SubmitOffersRequest(Builder builder) {
        this.offers = List.copyOf(builder.offers);
    }

    /** The offers to submit; never empty, at most 1000. */
    public List<Offer> offers() {
        return offers;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        offers.forEach(offer -> builder.addOffer(
                offer.offerId(), offer.marketplaceId(), offer.maxContributionPercentage()));
        return builder;
    }

    /** Fluent builder for {@link SubmitOffersRequest}; enforces the 1..1000 offer bound. */
    public static final class Builder {

        private final List<Offer> offers = new ArrayList<>();

        private Builder() {
        }

        /** Add an offer without a subsidy contribution declaration. */
        public Builder addOffer(String offerId, String marketplaceId) {
            return addOffer(offerId, marketplaceId, null);
        }

        /** Add an offer with the seller's maximum subsidy contribution percentage. */
        public Builder addOffer(String offerId, String marketplaceId,
                @Nullable String maxContributionPercentage) {
            offers.add(new Offer(offerId, marketplaceId, maxContributionPercentage));
            return this;
        }

        /**
         * Validate and build the request.
         *
         * @return the immutable request
         * @throws IllegalStateException if the offer count is outside 1..1000
         */
        public SubmitOffersRequest build() {
            SubsidyOfferBounds.check(offers.size());
            return new SubmitOffersRequest(this);
        }
    }
}
