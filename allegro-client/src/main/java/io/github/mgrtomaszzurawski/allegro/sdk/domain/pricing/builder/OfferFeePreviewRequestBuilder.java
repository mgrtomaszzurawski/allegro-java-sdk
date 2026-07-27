/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ClassifiedsPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreviewSellingMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link OfferFeePreviewRequest}. The category and a selling
 * mode are required; everything else is optional and set only when it changes
 * the fee. {@link #build()} validates the required fields fail-fast.
 *
 * <p>The common fixed-price case sets the price directly with
 * {@link #price(Money)}; other formats (and a net price) are set with
 * {@link #sellingMode(FeePreviewSellingMode)}.
 *
 * @since 0.1.0
 */
public final class OfferFeePreviewRequestBuilder {

    private static final String ERR_CATEGORY_REQUIRED = "categoryId is required";
    private static final String ERR_SELLING_MODE_REQUIRED =
            "a selling mode is required — set a price or a selling mode";

    private @Nullable String categoryId;
    private @Nullable FeePreviewSellingMode sellingMode;
    private @Nullable String offerId;
    private @Nullable String marketplaceId;
    private @Nullable String fundraisingCampaignId;
    private @Nullable String publicationDuration;
    private boolean emphasized1d;
    private boolean emphasized10d;
    private boolean departmentPage;
    private final List<OfferParameter> parameters = new ArrayList<>();
    private @Nullable ClassifiedsPackages classifiedsPackages;

    /**
     * Set the category the offer would be listed in (required).
     *
     * @param offerCategoryId the category id
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder categoryId(String offerCategoryId) {
        this.categoryId = offerCategoryId;
        return this;
    }

    /**
     * Set the selling format and its price (required). Use this for an auction
     * or a net price; the fixed-price case can use {@link #price(Money)} instead.
     *
     * @param mode the selling mode
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder sellingMode(FeePreviewSellingMode mode) {
        this.sellingMode = mode;
        return this;
    }

    /**
     * Set a fixed-price (Buy Now) selling mode at {@code buyNowPrice} — a
     * shortcut for {@code sellingMode(FeePreviewSellingMode.buyNow(price))}.
     *
     * @param buyNowPrice the Buy Now price
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder price(Money buyNowPrice) {
        return sellingMode(FeePreviewSellingMode.buyNow(buyNowPrice));
    }

    /**
     * Set an existing offer to preview fees for (optional).
     *
     * @param existingOfferId the offer id, or {@code null} for a new offer
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder offerId(@Nullable String existingOfferId) {
        this.offerId = existingOfferId;
        return this;
    }

    /**
     * Set the marketplace the fees apply to (optional).
     *
     * @param feeMarketplaceId the marketplace id, or {@code null} for the default
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder marketplaceId(@Nullable String feeMarketplaceId) {
        this.marketplaceId = feeMarketplaceId;
        return this;
    }

    /**
     * Set a charity fundraising campaign the offer supports (optional).
     *
     * @param campaignId the campaign id, or {@code null} for none
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder fundraisingCampaignId(@Nullable String campaignId) {
        this.fundraisingCampaignId = campaignId;
        return this;
    }

    /**
     * Set the listing duration as an ISO-8601 period, for example {@code P30D}
     * (optional).
     *
     * @param duration the duration, or {@code null} for the category default
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder publicationDuration(@Nullable String duration) {
        this.publicationDuration = duration;
        return this;
    }

    /**
     * Preview fees with the offer highlighted for one day.
     *
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder emphasizedForOneDay() {
        this.emphasized1d = true;
        return this;
    }

    /**
     * Preview fees with the offer highlighted for ten days.
     *
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder emphasizedForTenDays() {
        this.emphasized10d = true;
        return this;
    }

    /**
     * Preview fees with the offer shown on the category department page.
     *
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder onDepartmentPage() {
        this.departmentPage = true;
        return this;
    }

    /**
     * Set all promotion options at once (optional). A {@code null} value clears
     * them to {@link PromotionOptions#NONE}.
     *
     * @param options the promotion options
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder promotionOptions(@Nullable PromotionOptions options) {
        PromotionOptions effective = options == null ? PromotionOptions.NONE : options;
        this.emphasized1d = effective.emphasized1d();
        this.emphasized10d = effective.emphasized10d();
        this.departmentPage = effective.departmentPage();
        return this;
    }

    /**
     * Add one category parameter value (optional; may be called repeatedly).
     *
     * @param parameter the parameter
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder addParameter(OfferParameter parameter) {
        this.parameters.add(Objects.requireNonNull(parameter, "parameter"));
        return this;
    }

    /**
     * Replace the category parameter values (optional).
     *
     * @param offerParameters the parameters
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder parameters(List<OfferParameter> offerParameters) {
        this.parameters.clear();
        this.parameters.addAll(offerParameters);
        return this;
    }

    /**
     * Set the classifieds packages for an advertisement offer (optional).
     *
     * @param packages the classifieds packages, or {@code null} for none
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder classifiedsPackages(@Nullable ClassifiedsPackages packages) {
        this.classifiedsPackages = packages;
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if the category id or the selling mode is
     *     missing
     */
    public OfferFeePreviewRequest build() {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalStateException(ERR_CATEGORY_REQUIRED);
        }
        if (sellingMode == null) {
            throw new IllegalStateException(ERR_SELLING_MODE_REQUIRED);
        }
        return new OfferFeePreviewRequest(
                categoryId,
                sellingMode,
                offerId,
                marketplaceId,
                fundraisingCampaignId,
                publicationDuration,
                new PromotionOptions(emphasized1d, emphasized10d, departmentPage),
                List.copyOf(parameters),
                classifiedsPackages);
    }
}
