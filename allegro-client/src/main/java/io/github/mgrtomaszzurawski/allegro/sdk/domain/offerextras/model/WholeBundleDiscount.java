/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleWholeBundleDiscountDTORaw;
import java.util.List;

/**
 * A flexible bundle's single discount applied to the whole bundle once at least
 * {@code minimumBoughtOffers} of its offers are bought together.
 *
 * @param minimumBoughtOffers how many of the bundle's offers must be bought for
 *     the discount to apply
 * @param marketplaceDiscounts the per-marketplace discount percentages; never
 *     {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record WholeBundleDiscount(int minimumBoughtOffers, List<MarketplaceDiscount> marketplaceDiscounts) {

    public WholeBundleDiscount {
        marketplaceDiscounts = List.copyOf(marketplaceDiscounts);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    static WholeBundleDiscount from(FlexibleBundleWholeBundleDiscountDTORaw raw) {
        return new WholeBundleDiscount(
                raw.getMinimumBoughtOffers(),
                FlexibleBundleMappers.marketplaceDiscounts(raw.getDiscounts()));
    }
}
