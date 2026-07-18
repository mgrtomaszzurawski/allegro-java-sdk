/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleSlotsDiscountDTORaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A flexible bundle's discount configuration — either one discount for the whole
 * bundle or a separate discount per slot, as indicated by {@link #type()}.
 *
 * @param type whether the discount is whole-bundle or per-slot
 * @param wholeBundle the whole-bundle discount when {@code type} is
 *     {@link FlexibleBundleDiscountType#WHOLE_BUNDLE_DISCOUNT}, otherwise
 *     {@code null}
 * @param slotDiscounts the per-slot discounts when {@code type} is
 *     {@link FlexibleBundleDiscountType#SLOT_DISCOUNT}; never {@code null},
 *     possibly empty
 *
 * @since 0.2.0
 */
public record FlexibleBundleDiscount(
        FlexibleBundleDiscountType type,
        @Nullable WholeBundleDiscount wholeBundle,
        List<SlotDiscount> slotDiscounts) {

    public FlexibleBundleDiscount {
        slotDiscounts = List.copyOf(slotDiscounts);
    }

    /** Map the generated Layer-1 discount DTO to the public record. */
    static FlexibleBundleDiscount from(FlexibleBundleDiscountDTORaw raw) {
        return new FlexibleBundleDiscount(
                FlexibleBundleMappers.discountType(raw.getType().name()),
                raw.getBundle() == null ? null : WholeBundleDiscount.from(raw.getBundle()),
                slotDiscountsFrom(raw.getSlot()));
    }

    private static List<SlotDiscount> slotDiscountsFrom(@Nullable FlexibleBundleSlotsDiscountDTORaw raw) {
        return raw == null || raw.getSlots() == null
                ? List.of()
                : raw.getSlots().stream().map(SlotDiscount::from).toList();
    }
}
