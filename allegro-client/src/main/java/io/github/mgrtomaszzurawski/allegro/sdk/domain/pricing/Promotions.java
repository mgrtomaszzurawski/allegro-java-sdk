/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionType;
import java.util.stream.Stream;

/**
 * Rebate promotions for the authenticated seller — reached via
 * {@code pricing().promotions()}. Define large-order discounts, wholesale price
 * lists and multipack rewards, and target them at chosen offers.
 *
 * <p>Creating a promotion requires the seller's base marketplace to be
 * {@code allegro-pl}. Reads need {@code sale:offers:read}; writes need
 * {@code sale:offers:write}.
 *
 * @since 0.4.0
 */
public interface Promotions {

    /**
     * Lazily stream the seller's promotions of the given type. The stream fetches
     * one page at a time as it is consumed; only what the caller materialises is
     * held in memory.
     *
     * @param type the promotion type to list (required by the endpoint)
     * @return a lazy stream over the matching promotions
     * @throws IllegalArgumentException if {@code type} is null
     */
    Stream<Promotion> streamPromotions(PromotionType type);

    /**
     * Lazily stream the seller's promotions of the given type that apply to one
     * offer.
     *
     * @param type the promotion type to list (required by the endpoint)
     * @param offerId the offer to filter by (sent as the {@code offer.id}
     *     filter); a {@code null} offer id yields the same result as
     *     {@link #streamPromotions(PromotionType)} (no offer filter)
     * @return a lazy stream over the matching promotions
     * @throws IllegalArgumentException if {@code type} is null
     */
    Stream<Promotion> streamPromotions(PromotionType type, String offerId);

    /**
     * Read one promotion by id.
     *
     * @param promotionId the promotion id
     * @return the promotion
     */
    Promotion get(String promotionId);

    /**
     * Create a new promotion.
     *
     * @param request the benefits and offer criteria, built with
     *     {@link PromotionRequest#builder()}
     * @return the created promotion (with its assigned id and status)
     */
    Promotion create(PromotionRequest request);

    /**
     * Modify an existing promotion, replacing its benefits and offer criteria.
     *
     * @param promotionId the promotion id
     * @param request the new benefits and offer criteria
     * @return the modified promotion
     */
    Promotion modify(String promotionId, PromotionRequest request);

    /**
     * Deactivate a promotion by id.
     *
     * @param promotionId the promotion id
     */
    void deactivate(String promotionId);
}
