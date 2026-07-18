/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingOfferRuleConfigurationPriceRangeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;

/**
 * The price band an automatic pricing rule is confined to for one offer: the
 * rule never sets the price below {@link #minPrice()} or above {@link #maxPrice()}.
 *
 * @param currency which currency the min/max prices are expressed in
 * @param minPrice the lowest price the rule may set
 * @param maxPrice the highest price the rule may set
 *
 * @since 0.3.0
 */
public record OfferRulePriceRange(PriceRangeCurrency currency, Money minPrice, Money maxPrice) {

    /**
     * Which currency an offer-rule price range is expressed in — Allegro spec
     * enum values kept verbatim.
     */
    public enum PriceRangeCurrency {

        /** Prices are in the offer's base-marketplace currency. */
        BASE_MARKETPLACE_CURRENCY,

        /** Prices are in the (non-base) marketplace's own currency. */
        MARKETPLACE_CURRENCY
    }

    /**
     * Map the generated price-range DTO to the public record.
     *
     * @param raw the generated price-range configuration DTO
     * @return the mapped record
     */
    public static OfferRulePriceRange from(AutomaticPricingOfferRuleConfigurationPriceRangeRaw raw) {
        return new OfferRulePriceRange(
                PriceRangeCurrency.valueOf(raw.getType().getValue()),
                Money.of(raw.getMinPrice().getAmount(), raw.getMinPrice().getCurrency()),
                Money.of(raw.getMaxPrice().getAmount(), raw.getMaxPrice().getCurrency()));
    }
}
