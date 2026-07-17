/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

/**
 * The strategy an automatic pricing rule follows when it recalculates an offer's
 * price.
 *
 * @since 0.2.0
 */
public enum PricingRuleType {

    /**
     * Convert the price from the offer's base marketplace using the latest
     * exchange rate. Not available on the base or on business marketplaces.
     */
    EXCHANGE_RATE,

    /** Follow the lowest price for the product on Allegro. */
    FOLLOW_BY_ALLEGRO_MIN_PRICE,

    /** Follow the lowest price for the product on the market. */
    FOLLOW_BY_MARKET_MIN_PRICE,

    /** Follow the price of the current top offer for the product. */
    FOLLOW_BY_TOP_OFFER_PRICE
}
