/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * A request to submit one offer to an AlleDiscount campaign, passed to
 * {@code alleDiscount().submitOffer(...)}. All three fields are required; the
 * {@code proposedPrice} must not exceed the offer's {@code requiredMerchantPrice}
 * (read it from {@code streamEligibleOffers}).
 *
 * <pre>{@code
 * SubmitOfferRequest request = SubmitOfferRequest.builder()
 *         .campaignId("winter-sale")
 *         .offerId("12345678")
 *         .proposedPrice(Money.of("24.99", "PLN"))
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class SubmitOfferRequest {

    private final String campaignId;
    private final String offerId;
    private final Money proposedPrice;

    private SubmitOfferRequest(Builder builder) {
        this.campaignId = builder.campaignId;
        this.offerId = builder.offerId;
        this.proposedPrice = builder.proposedPrice;
    }

    /** The campaign to submit the offer to. */
    public String campaignId() {
        return campaignId;
    }

    /** The offer to submit. */
    public String offerId() {
        return offerId;
    }

    /** The seller's proposed price. */
    public Money proposedPrice() {
        return proposedPrice;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder()
                .campaignId(campaignId)
                .offerId(offerId)
                .proposedPrice(proposedPrice);
    }

    /** Fluent builder for {@link SubmitOfferRequest}; validates required fields fail-fast. */
    public static final class Builder {

        private static final String ERR_CAMPAIGN_REQUIRED = "campaignId is required";
        private static final String ERR_OFFER_REQUIRED = "offerId is required";
        private static final String ERR_PRICE_REQUIRED = "proposedPrice is required";

        private @Nullable String campaignId;
        private @Nullable String offerId;
        private @Nullable Money proposedPrice;

        private Builder() {
        }

        /** Set the campaign to submit to (required). */
        public Builder campaignId(@Nullable String alleDiscountCampaignId) {
            this.campaignId = alleDiscountCampaignId;
            return this;
        }

        /** Set the offer to submit (required). */
        public Builder offerId(@Nullable String alleDiscountOfferId) {
            this.offerId = alleDiscountOfferId;
            return this;
        }

        /** Set the seller's proposed price (required). */
        public Builder proposedPrice(@Nullable Money price) {
            this.proposedPrice = price;
            return this;
        }

        /**
         * Validate and build the request.
         *
         * @return the immutable request
         * @throws IllegalStateException if any required field is missing or blank
         */
        public SubmitOfferRequest build() {
            if (campaignId == null || campaignId.isBlank()) {
                throw new IllegalStateException(ERR_CAMPAIGN_REQUIRED);
            }
            if (offerId == null || offerId.isBlank()) {
                throw new IllegalStateException(ERR_OFFER_REQUIRED);
            }
            if (proposedPrice == null) {
                throw new IllegalStateException(ERR_PRICE_REQUIRED);
            }
            return new SubmitOfferRequest(this);
        }
    }
}
