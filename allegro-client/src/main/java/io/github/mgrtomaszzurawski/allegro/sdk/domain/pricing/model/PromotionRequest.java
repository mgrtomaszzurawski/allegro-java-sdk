/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.PromotionRequestBuilder;
import java.util.List;

/**
 * The payload to create or modify a rebate {@link Promotion}: the
 * {@link Benefit benefits} to grant and the {@link OfferCriterion criteria} that
 * select which offers they apply to. Build it with {@link #builder()}.
 *
 * @param benefits the rewards to grant (at least one)
 * @param offerCriteria which offers the promotion applies to (at least one)
 *
 * @since 0.4.0
 */
public record PromotionRequest(List<Benefit> benefits, List<OfferCriterion> offerCriteria) {

    /** Defensively copies the lists so the request stays immutable. */
    public PromotionRequest {
        benefits = List.copyOf(benefits);
        offerCriteria = List.copyOf(offerCriteria);
    }

    /**
     * Start building a promotion request.
     *
     * @return a fresh builder
     */
    public static PromotionRequestBuilder builder() {
        return new PromotionRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's benefits and criteria.
     *
     * @return a builder seeded from this request
     */
    public PromotionRequestBuilder toBuilder() {
        return new PromotionRequestBuilder()
                .benefits(benefits)
                .offerCriteria(offerCriteria);
    }
}
