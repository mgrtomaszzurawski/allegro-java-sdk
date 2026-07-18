/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link OfferFeePreviewRequest}. The {@code categoryId} and
 * {@code price} are required; {@code offerId} is optional. {@link #build()}
 * validates the required fields fail-fast.
 *
 * @since 0.3.0
 */
public final class OfferFeePreviewRequestBuilder {

    private static final String ERR_CATEGORY_REQUIRED = "categoryId is required";
    private static final String ERR_PRICE_REQUIRED = "price is required";

    private @Nullable String categoryId;
    private @Nullable Money price;
    private @Nullable String offerId;

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
     * Set the Buy Now price to preview fees for (required).
     *
     * @param buyNowPrice the price
     * @return this builder
     */
    public OfferFeePreviewRequestBuilder price(Money buyNowPrice) {
        this.price = buyNowPrice;
        return this;
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
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if the category id or price is missing
     */
    public OfferFeePreviewRequest build() {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalStateException(ERR_CATEGORY_REQUIRED);
        }
        if (price == null) {
            throw new IllegalStateException(ERR_PRICE_REQUIRED);
        }
        return new OfferFeePreviewRequest(categoryId, price, offerId);
    }
}
