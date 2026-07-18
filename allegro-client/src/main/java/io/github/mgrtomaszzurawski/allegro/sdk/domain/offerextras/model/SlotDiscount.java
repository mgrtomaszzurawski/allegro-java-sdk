/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleSlotDiscountDTORaw;
import java.util.List;

/**
 * A flexible bundle's discount for one slot.
 *
 * @param order the position (0-based) of the slot the discount applies to
 * @param marketplaceDiscounts the per-marketplace discount percentages; never
 *     {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record SlotDiscount(int order, List<MarketplaceDiscount> marketplaceDiscounts) {

    public SlotDiscount {
        marketplaceDiscounts = List.copyOf(marketplaceDiscounts);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    static SlotDiscount from(FlexibleBundleSlotDiscountDTORaw raw) {
        return new SlotDiscount(
                raw.getOrder(),
                FlexibleBundleMappers.marketplaceDiscounts(raw.getDiscounts()));
    }
}
