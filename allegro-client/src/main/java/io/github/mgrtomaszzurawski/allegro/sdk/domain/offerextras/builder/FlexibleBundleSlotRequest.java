/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One slot of a flexible bundle to create or update: its position, whether it is
 * the bundle's entry point, how many units it requires, and the offers a buyer may
 * choose for it. Build it with {@link #builder()}.
 *
 * @param id the slot identifier — {@code null} for a new slot, set when updating an
 *     existing slot
 * @param order the position of the slot in the bundle (0-based)
 * @param entryPoint whether this slot is the bundle's entry point
 * @param requiredQuantity how many units the slot requires
 * @param offers the offers a buyer may choose for this slot; never {@code null},
 *     never empty
 *
 * @since 0.2.0
 */
public record FlexibleBundleSlotRequest(
        @Nullable String id,
        int order,
        boolean entryPoint,
        int requiredQuantity,
        List<FlexibleBundleOfferRef> offers) {

    public FlexibleBundleSlotRequest {
        offers = List.copyOf(offers);
    }

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link FlexibleBundleSlotRequestBuilder}
     */
    public static FlexibleBundleSlotRequestBuilder builder() {
        return new FlexibleBundleSlotRequestBuilder();
    }

    /**
     * A builder pre-populated with this slot's fields.
     *
     * @return a builder holding this slot's values
     */
    public FlexibleBundleSlotRequestBuilder toBuilder() {
        return new FlexibleBundleSlotRequestBuilder()
                .id(id)
                .order(order)
                .entryPoint(entryPoint)
                .requiredQuantity(requiredQuantity)
                .offers(offers);
    }
}
