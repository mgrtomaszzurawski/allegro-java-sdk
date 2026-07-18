/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerFeaturesRaw;

/**
 * Read-only flags describing how a shipping-rate set is managed.
 *
 * @param managedByAllegro whether Allegro manages the set (the seller cannot edit it)
 * @param fulfillment whether the set belongs to Allegro's fulfilment programme
 *
 * @since 0.3.0
 */
public record RateSetFeatures(boolean managedByAllegro, boolean fulfillment) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static RateSetFeatures from(
            GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerFeaturesRaw raw) {
        return new RateSetFeatures(
                Boolean.TRUE.equals(raw.getManagedByAllegro()),
                Boolean.TRUE.equals(raw.getIsFulfillment()));
    }
}
