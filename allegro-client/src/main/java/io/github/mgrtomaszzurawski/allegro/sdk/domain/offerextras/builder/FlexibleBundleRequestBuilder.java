/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscount;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link FlexibleBundleRequest}. At least one slot is required;
 * {@link #build()} validates it fail-fast. The discount is optional.
 *
 * @since 0.2.0
 */
public final class FlexibleBundleRequestBuilder {

    private static final String ERR_NO_SLOTS = "a flexible bundle must have at least one slot";

    private List<FlexibleBundleSlotRequest> slots = List.of();
    private @Nullable FlexibleBundleDiscount discount;

    /**
     * Set the bundle's slots (at least one; required).
     *
     * @param bundleSlots the bundle's slots
     * @return this builder
     */
    public FlexibleBundleRequestBuilder slots(List<FlexibleBundleSlotRequest> bundleSlots) {
        this.slots = List.copyOf(bundleSlots);
        return this;
    }

    /**
     * Add one slot to the bundle.
     *
     * @param slot the slot to add
     * @return this builder
     */
    public FlexibleBundleRequestBuilder slot(FlexibleBundleSlotRequest slot) {
        List<FlexibleBundleSlotRequest> next = new ArrayList<>(slots);
        next.add(slot);
        this.slots = List.copyOf(next);
        return this;
    }

    /**
     * Set the bundle's discount, or {@code null} for no bundle discount.
     *
     * @param bundleDiscount the discount configuration
     * @return this builder
     */
    public FlexibleBundleRequestBuilder discount(@Nullable FlexibleBundleDiscount bundleDiscount) {
        this.discount = bundleDiscount;
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if the bundle has no slots
     */
    public FlexibleBundleRequest build() {
        if (slots.isEmpty()) {
            throw new IllegalStateException(ERR_NO_SLOTS);
        }
        return new FlexibleBundleRequest(slots, discount);
    }
}
