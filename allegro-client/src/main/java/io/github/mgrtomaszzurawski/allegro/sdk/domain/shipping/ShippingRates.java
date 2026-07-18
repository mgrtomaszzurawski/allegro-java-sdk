/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import java.util.List;

/**
 * The seller's shipping-rate sets — reached via {@code shipping.rates()}. A rate
 * set groups per-delivery-method price rows a seller attaches to their offers.
 * Reads need {@code sale:settings:read}; {@link #create(ShippingRateSetRequest)}
 * and {@link #update(String, ShippingRateSetRequest)} need
 * {@code sale:settings:write}.
 *
 * @since 0.3.0
 */
public interface ShippingRates {

    /**
     * List the seller's shipping-rate sets as summaries (no rate rows). The
     * response is not paginated, so this returns a plain {@link List}.
     *
     * @return the rate-set summaries, possibly empty
     */
    List<ShippingRateSetSummary> list();

    /**
     * Fetch one shipping-rate set in full, including its rate rows.
     *
     * @param rateSetId the rate-set id
     * @return the full rate set
     */
    ShippingRateSet get(String rateSetId);

    /**
     * Create a new shipping-rate set.
     *
     * @param request the set to create
     * @return the created set as stored (with its server-assigned id)
     */
    ShippingRateSet create(ShippingRateSetRequest request);

    /**
     * Replace an existing shipping-rate set (PUT semantics — send the full
     * desired state).
     *
     * @param rateSetId the rate-set id to replace
     * @param request the new set state
     * @return the set as stored after the update
     */
    ShippingRateSet update(String rateSetId, ShippingRateSetRequest request);
}
