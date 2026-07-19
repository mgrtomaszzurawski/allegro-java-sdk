/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerMarketplacesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A summary row from {@code shipping.rates().list()} — enough to identify a
 * shipping-rate set and know where it applies, without the per-method rate rows.
 * Fetch the full set (with its rates) via {@code shipping.rates().get(id)}.
 *
 * @param id the rate-set identifier
 * @param name the seller's name for the set
 * @param features management flags for the set, or {@code null} when absent
 * @param marketplaces ids of the marketplaces the set applies to; never
 *     {@code null}, possibly empty
 *
 * @since 0.3.0
 */
public record ShippingRateSetSummary(
        String id,
        String name,
        @Nullable RateSetFeatures features,
        List<String> marketplaces) {

    public ShippingRateSetSummary {
        marketplaces = List.copyOf(marketplaces);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static ShippingRateSetSummary from(
            GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerRaw raw) {
        return new ShippingRateSetSummary(
                raw.getId(),
                raw.getName(),
                raw.getFeatures() == null ? null : RateSetFeatures.from(raw.getFeatures()),
                marketplaces(raw.getMarketplaces()));
    }

    private static List<String> marketplaces(
            @Nullable List<GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerMarketplacesInnerRaw> raw) {
        return raw == null ? List.of()
                : raw.stream()
                        .map(GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerMarketplacesInnerRaw::getId)
                        .toList();
    }
}
