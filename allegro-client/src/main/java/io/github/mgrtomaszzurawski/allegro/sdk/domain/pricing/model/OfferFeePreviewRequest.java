/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.OfferFeePreviewRequestBuilder;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The draft-offer details needed to preview its fees: what it would cost to list
 * and sell one item in {@code categoryId} under a given selling mode. Build it
 * with {@link #builder()}, which validates the required fields fail-fast.
 *
 * <p>Only the category and selling mode are required. The remaining inputs are
 * optional and are supplied when they change the fee: an existing
 * {@code offerId}, the {@code marketplaceId} the fees apply to, paid
 * {@code promotionOptions}, the {@code publicationDuration}, category
 * {@code parameters}, {@code classifiedsPackages} for advertisement categories,
 * and a {@code fundraisingCampaignId} for a charity offer.
 *
 * @param categoryId the category the offer would be listed in (required)
 * @param sellingMode the selling format and its price (required)
 * @param offerId an existing offer to preview fees for, or {@code null} for a
 *     hypothetical new offer
 * @param marketplaceId the marketplace the fees apply to, or {@code null} for
 *     the seller's default marketplace
 * @param fundraisingCampaignId a charity fundraising campaign the offer supports,
 *     or {@code null}
 * @param publicationDuration the listing duration as an ISO-8601 period (for
 *     example {@code P30D}), or {@code null} for the category default
 * @param promotionOptions the paid promotion options (never {@code null};
 *     {@link PromotionOptions#NONE} when none)
 * @param parameters the category parameter values (never {@code null}; empty
 *     when none)
 * @param classifiedsPackages the classifieds packages, or {@code null} for a
 *     non-advertisement offer
 *
 * @since 0.1.0
 */
public record OfferFeePreviewRequest(
        String categoryId,
        FeePreviewSellingMode sellingMode,
        @Nullable String offerId,
        @Nullable String marketplaceId,
        @Nullable String fundraisingCampaignId,
        @Nullable String publicationDuration,
        PromotionOptions promotionOptions,
        List<OfferParameter> parameters,
        @Nullable ClassifiedsPackages classifiedsPackages) {

    /**
     * Compact constructor validating required fields, defaulting the promotion
     * options and taking a defensive copy of the parameters.
     */
    public OfferFeePreviewRequest {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(sellingMode, "sellingMode");
        promotionOptions = promotionOptions == null ? PromotionOptions.NONE : promotionOptions;
        parameters = List.copyOf(parameters);
    }

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link OfferFeePreviewRequestBuilder}
     */
    public static OfferFeePreviewRequestBuilder builder() {
        return new OfferFeePreviewRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's fields, for deriving a
     * modified copy.
     *
     * @return a builder holding this request's values
     */
    public OfferFeePreviewRequestBuilder toBuilder() {
        return new OfferFeePreviewRequestBuilder()
                .categoryId(categoryId)
                .sellingMode(sellingMode)
                .offerId(offerId)
                .marketplaceId(marketplaceId)
                .fundraisingCampaignId(fundraisingCampaignId)
                .publicationDuration(publicationDuration)
                .promotionOptions(promotionOptions)
                .parameters(parameters)
                .classifiedsPackages(classifiedsPackages);
    }

    /**
     * The Buy Now price when this request previews a fixed-price offer, for the
     * common case that reads back the price it set with
     * {@link OfferFeePreviewRequestBuilder#price(Money)}.
     *
     * @return the Buy Now price, or {@code null} when the selling mode is not
     *     {@link FeePreviewSellingMode.BuyNow}
     */
    public @Nullable Money buyNowPrice() {
        return sellingMode instanceof FeePreviewSellingMode.BuyNow buyNow ? buyNow.price() : null;
    }
}
