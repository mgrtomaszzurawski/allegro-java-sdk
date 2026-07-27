/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleGetOfferDTORaw;
import java.util.List;

/**
 * One offer within a flexible bundle's slot.
 *
 * @param offerId the offer identifier
 * @param excludedFromDiscount whether this offer is excluded from the bundle
 *     discount
 * @param entryPoint whether this offer is a bundle entry point
 * @param marketplaces per-marketplace availability of this offer in the bundle;
 *     never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record FlexibleBundleSlotOffer(
        String offerId,
        boolean excludedFromDiscount,
        boolean entryPoint,
        List<FlexibleBundleOfferMarketplace> marketplaces) {

    public FlexibleBundleSlotOffer {
        marketplaces = marketplaces == null ? List.of() : List.copyOf(marketplaces);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    static FlexibleBundleSlotOffer from(FlexibleBundleGetOfferDTORaw raw) {
        return new FlexibleBundleSlotOffer(
                raw.getId(),
                raw.getExcludedFromDiscount(),
                raw.getEntryPoint(),
                raw.getMarketplaces() == null
                        ? List.of()
                        : raw.getMarketplaces().stream().map(FlexibleBundleOfferMarketplace::from).toList());
    }
}
