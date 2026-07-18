/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * A request to apply a campaign badge to one of the seller's offers, passed to
 * {@code badges().apply(...)}. The {@code campaignId} and {@code offerId} are
 * required; the bargain price, per-buyer purchase limit and declared stock are
 * optional and depend on the campaign's rules.
 *
 * <pre>{@code
 * BadgeApplicationRequest request = BadgeApplicationRequest.builder()
 *         .campaignId("allegro-bargain")
 *         .offerId("12345678")
 *         .bargainPrice(Money.of("29.99", "PLN"))
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class BadgeApplicationRequest {

    private final String campaignId;
    private final String offerId;
    private final @Nullable Money bargainPrice;
    private final @Nullable Integer purchaseLimitPerUser;
    private final @Nullable BigDecimal declaredStockQuantity;

    private BadgeApplicationRequest(Builder builder) {
        this.campaignId = builder.campaignId;
        this.offerId = builder.offerId;
        this.bargainPrice = builder.bargainPrice;
        this.purchaseLimitPerUser = builder.purchaseLimitPerUser;
        this.declaredStockQuantity = builder.declaredStockQuantity;
    }

    /** The badge campaign to apply for. */
    public String campaignId() {
        return campaignId;
    }

    /** The offer the badge is requested on. */
    public String offerId() {
        return offerId;
    }

    /** The declared bargain price, or {@code null} if not set. */
    public @Nullable Money bargainPrice() {
        return bargainPrice;
    }

    /** The per-buyer purchase limit, or {@code null} if not set. */
    public @Nullable Integer purchaseLimitPerUser() {
        return purchaseLimitPerUser;
    }

    /** The stock declared for the campaign, or {@code null} if not set. */
    public @Nullable BigDecimal declaredStockQuantity() {
        return declaredStockQuantity;
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
                .bargainPrice(bargainPrice)
                .purchaseLimitPerUser(purchaseLimitPerUser)
                .declaredStockQuantity(declaredStockQuantity);
    }

    /** Fluent builder for {@link BadgeApplicationRequest}; validates required fields fail-fast. */
    public static final class Builder {

        private static final String ERR_CAMPAIGN_REQUIRED = "campaignId is required";
        private static final String ERR_OFFER_REQUIRED = "offerId is required";

        private @Nullable String campaignId;
        private @Nullable String offerId;
        private @Nullable Money bargainPrice;
        private @Nullable Integer purchaseLimitPerUser;
        private @Nullable BigDecimal declaredStockQuantity;

        private Builder() {
        }

        /** Set the badge campaign to apply for (required). */
        public Builder campaignId(@Nullable String badgeCampaignId) {
            this.campaignId = badgeCampaignId;
            return this;
        }

        /** Set the offer the badge is requested on (required). */
        public Builder offerId(@Nullable String badgeOfferId) {
            this.offerId = badgeOfferId;
            return this;
        }

        /** Set the declared bargain price (optional). */
        public Builder bargainPrice(@Nullable Money price) {
            this.bargainPrice = price;
            return this;
        }

        /** Set the per-buyer purchase limit (optional). */
        public Builder purchaseLimitPerUser(@Nullable Integer maxItemsPerUser) {
            this.purchaseLimitPerUser = maxItemsPerUser;
            return this;
        }

        /** Set the stock declared for the campaign (optional). */
        public Builder declaredStockQuantity(@Nullable BigDecimal quantity) {
            this.declaredStockQuantity = quantity;
            return this;
        }

        /**
         * Validate and build the request.
         *
         * @return the immutable request
         * @throws IllegalStateException if {@code campaignId} or {@code offerId} is missing or blank
         */
        public BadgeApplicationRequest build() {
            if (campaignId == null || campaignId.isBlank()) {
                throw new IllegalStateException(ERR_CAMPAIGN_REQUIRED);
            }
            if (offerId == null || offerId.isBlank()) {
                throw new IllegalStateException(ERR_OFFER_REQUIRED);
            }
            return new BadgeApplicationRequest(this);
        }
    }
}
