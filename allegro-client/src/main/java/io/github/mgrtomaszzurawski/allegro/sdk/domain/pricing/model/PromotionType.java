/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

/**
 * The kind of rebate promotion to list — the required filter for
 * {@link io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Promotions#streamPromotions(PromotionType)}.
 *
 * <p>These are Allegro spec enum values kept verbatim; they are the vocabulary
 * of the {@code promotionType} query filter, which is a superset of the benefit
 * discriminators (a multipack promotion may confine its discount to a single
 * offer or span several).
 *
 * @since 0.4.0
 */
public enum PromotionType {

    /** A "buy several of one offer, get a discount" multipack promotion. */
    MULTIPACK,

    /** A multipack promotion whose quantity is counted across several offers. */
    CROSS_MULTIPACK,

    /** A discount that grows in tiers with the total order value. */
    LARGE_ORDER_DISCOUNT,

    /** A quantity-tiered wholesale price list. */
    WHOLESALE_PRICE_LIST
}
