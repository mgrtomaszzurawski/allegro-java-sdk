/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * The offers to exclude from Allegro Prices, passed to
 * {@code allegroPrices().excludeOffers(...)}. Between 1 and 1000 offers per command.
 *
 * <pre>{@code
 * ExcludeOffersRequest request = ExcludeOffersRequest.builder()
 *         .addOffer("12345678", "allegro-pl")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ExcludeOffersRequest {

    /**
     * One offer to exclude.
     *
     * @param offerId       the offer
     * @param marketplaceId the marketplace to exclude it on
     */
    public record Offer(String offerId, String marketplaceId) {
    }

    private final List<Offer> offers;

    private ExcludeOffersRequest(Builder builder) {
        this.offers = List.copyOf(builder.offers);
    }

    /** The offers to exclude; never empty, at most 1000. */
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
        offers.forEach(offer -> builder.addOffer(offer.offerId(), offer.marketplaceId()));
        return builder;
    }

    /** Fluent builder for {@link ExcludeOffersRequest}; enforces the 1..1000 offer bound. */
    public static final class Builder {

        private final List<Offer> offers = new ArrayList<>();

        private Builder() {
        }

        /** Add an offer to exclude. */
        public Builder addOffer(String offerId, String marketplaceId) {
            offers.add(new Offer(offerId, marketplaceId));
            return this;
        }

        /**
         * Validate and build the request.
         *
         * @return the immutable request
         * @throws IllegalStateException if the offer count is outside 1..1000
         */
        public ExcludeOffersRequest build() {
            SubsidyOfferBounds.check(offers.size());
            return new ExcludeOffersRequest(this);
        }
    }
}
