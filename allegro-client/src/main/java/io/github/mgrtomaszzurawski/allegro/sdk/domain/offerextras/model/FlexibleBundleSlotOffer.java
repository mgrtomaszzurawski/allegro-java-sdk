/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleGetOfferDTORaw;

/**
 * One offer within a flexible bundle's slot.
 *
 * @param offerId the offer identifier
 * @param excludedFromDiscount whether this offer is excluded from the bundle
 *     discount
 * @param entryPoint whether this offer is a bundle entry point
 *
 * @since 0.2.0
 */
public record FlexibleBundleSlotOffer(String offerId, boolean excludedFromDiscount, boolean entryPoint) {

    /** Map the generated Layer-1 DTO to the public record. */
    static FlexibleBundleSlotOffer from(FlexibleBundleGetOfferDTORaw raw) {
        return new FlexibleBundleSlotOffer(raw.getId(), raw.getExcludedFromDiscount(), raw.getEntryPoint());
    }
}
