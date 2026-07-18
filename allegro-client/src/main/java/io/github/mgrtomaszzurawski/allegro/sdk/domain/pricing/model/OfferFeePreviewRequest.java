/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.OfferFeePreviewRequestBuilder;
import org.jspecify.annotations.Nullable;

/**
 * The draft-offer details needed to preview its fees: what it would cost to sell
 * one item in {@code categoryId} at {@code price} under the Buy Now format. Build
 * it with {@link #builder()}, which validates the required fields fail-fast.
 *
 * <p>This is a deliberately focused Buy-Now request — it carries only the inputs
 * the fee calculation needs (category and price, plus an optional existing offer
 * id) rather than the full offer shape, so the pricing bucket stays decoupled
 * from the offers bucket.
 *
 * @param categoryId the category the offer would be listed in (required)
 * @param price the Buy Now price (required)
 * @param offerId an existing offer to preview fees for, or {@code null} for a
 *     hypothetical new offer
 *
 * @since 0.3.0
 */
public record OfferFeePreviewRequest(
        String categoryId,
        Money price,
        @Nullable String offerId) {

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
                .price(price)
                .offerId(offerId);
    }
}
