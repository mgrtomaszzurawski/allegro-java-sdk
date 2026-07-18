/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Benefit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferCriterion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link PromotionRequest}. A promotion needs at least one
 * {@link Benefit benefit} and at least one {@link OfferCriterion criterion};
 * {@link #build()} fails fast if either is missing, since a promotion with no
 * reward or no target offers rewards nothing.
 *
 * @since 0.4.0
 */
public final class PromotionRequestBuilder {

    private static final String ERR_NO_BENEFIT = "at least one benefit is required";
    private static final String ERR_NO_CRITERION = "at least one offer criterion is required";

    private final List<Benefit> benefits = new ArrayList<>();
    private final List<OfferCriterion> offerCriteria = new ArrayList<>();

    /**
     * Append one benefit.
     *
     * @param benefit the reward to grant
     * @return this builder
     */
    public PromotionRequestBuilder addBenefit(Benefit benefit) {
        this.benefits.add(benefit);
        return this;
    }

    /**
     * Replace the benefits with the given list.
     *
     * @param newBenefits the rewards to grant
     * @return this builder
     */
    public PromotionRequestBuilder benefits(List<Benefit> newBenefits) {
        this.benefits.clear();
        this.benefits.addAll(newBenefits);
        return this;
    }

    /**
     * Append one offer criterion.
     *
     * @param criterion the offer-selection criterion
     * @return this builder
     */
    public PromotionRequestBuilder addOfferCriterion(OfferCriterion criterion) {
        this.offerCriteria.add(criterion);
        return this;
    }

    /**
     * Replace the offer criteria with the given list.
     *
     * @param newCriteria the offer-selection criteria
     * @return this builder
     */
    public PromotionRequestBuilder offerCriteria(List<OfferCriterion> newCriteria) {
        this.offerCriteria.clear();
        this.offerCriteria.addAll(newCriteria);
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if no benefit or no criterion was supplied
     */
    public PromotionRequest build() {
        if (benefits.isEmpty()) {
            throw new IllegalStateException(ERR_NO_BENEFIT);
        }
        if (offerCriteria.isEmpty()) {
            throw new IllegalStateException(ERR_NO_CRITERION);
        }
        return new PromotionRequest(benefits, offerCriteria);
    }
}
