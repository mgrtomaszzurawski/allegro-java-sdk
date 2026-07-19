/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleSlotsDiscountDTORaw;
import java.util.List;
import java.util.Objects;
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

    private static final String ERR_WHOLE_BUNDLE_NULL = "wholeBundle must not be null";
    private static final String ERR_SLOT_DISCOUNTS_NULL = "slotDiscounts must not be null";

    public FlexibleBundleDiscount {
        slotDiscounts = List.copyOf(slotDiscounts);
    }

    /**
     * A whole-bundle discount, applied once across the bundle. Use this when
     * creating or updating a flexible bundle.
     *
     * @param wholeBundle the whole-bundle discount configuration
     * @return a {@link FlexibleBundleDiscountType#WHOLE_BUNDLE_DISCOUNT} discount
     */
    public static FlexibleBundleDiscount wholeBundle(WholeBundleDiscount wholeBundle) {
        Objects.requireNonNull(wholeBundle, ERR_WHOLE_BUNDLE_NULL);
        return new FlexibleBundleDiscount(FlexibleBundleDiscountType.WHOLE_BUNDLE_DISCOUNT, wholeBundle, List.of());
    }

    /**
     * A per-slot discount, configured separately for each slot. Use this when
     * creating or updating a flexible bundle.
     *
     * @param slotDiscounts the per-slot discounts
     * @return a {@link FlexibleBundleDiscountType#SLOT_DISCOUNT} discount
     */
    public static FlexibleBundleDiscount perSlot(List<SlotDiscount> slotDiscounts) {
        Objects.requireNonNull(slotDiscounts, ERR_SLOT_DISCOUNTS_NULL);
        return new FlexibleBundleDiscount(FlexibleBundleDiscountType.SLOT_DISCOUNT, null, slotDiscounts);
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
