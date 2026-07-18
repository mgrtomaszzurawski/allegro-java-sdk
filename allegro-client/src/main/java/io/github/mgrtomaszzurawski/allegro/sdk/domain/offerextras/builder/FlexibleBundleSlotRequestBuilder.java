/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link FlexibleBundleSlotRequest}. The order, required
 * quantity, and at least one offer are mandatory; {@link #build()} validates them
 * fail-fast. The slot id is optional (set only when updating an existing slot) and
 * the entry-point flag defaults to {@code false}.
 *
 * @since 0.2.0
 */
public final class FlexibleBundleSlotRequestBuilder {

    private static final int MIN_ORDER = 0;
    private static final int MIN_REQUIRED_QUANTITY = 1;

    private static final String ERR_ORDER_REQUIRED = "order is required";
    private static final String ERR_ORDER_NEGATIVE = "order must be 0 or greater";
    private static final String ERR_QUANTITY_REQUIRED = "requiredQuantity is required";
    private static final String ERR_QUANTITY_MIN = "requiredQuantity must be 1 or greater";
    private static final String ERR_NO_OFFERS = "a slot must have at least one offer";

    private @Nullable String id;
    private @Nullable Integer order;
    private boolean entryPoint;
    private @Nullable Integer requiredQuantity;
    private List<FlexibleBundleOfferRef> offers = List.of();

    /**
     * Set the slot id — only when updating an existing slot; leave unset for a new
     * slot.
     *
     * @param slotId the slot identifier
     * @return this builder
     */
    public FlexibleBundleSlotRequestBuilder id(@Nullable String slotId) {
        this.id = slotId;
        return this;
    }

    /**
     * Set the slot's position in the bundle (0-based; required).
     *
     * @param slotOrder the 0-based position
     * @return this builder
     */
    public FlexibleBundleSlotRequestBuilder order(@Nullable Integer slotOrder) {
        this.order = slotOrder;
        return this;
    }

    /**
     * Set whether this slot is the bundle's entry point (defaults to
     * {@code false}).
     *
     * @param isEntryPoint whether this is the entry-point slot
     * @return this builder
     */
    public FlexibleBundleSlotRequestBuilder entryPoint(boolean isEntryPoint) {
        this.entryPoint = isEntryPoint;
        return this;
    }

    /**
     * Set how many units the slot requires (1 or greater; required).
     *
     * @param quantity the required quantity
     * @return this builder
     */
    public FlexibleBundleSlotRequestBuilder requiredQuantity(@Nullable Integer quantity) {
        this.requiredQuantity = quantity;
        return this;
    }

    /**
     * Set the offers a buyer may choose for this slot (at least one; required).
     *
     * @param slotOffers the slot's offers
     * @return this builder
     */
    public FlexibleBundleSlotRequestBuilder offers(List<FlexibleBundleOfferRef> slotOffers) {
        this.offers = List.copyOf(slotOffers);
        return this;
    }

    /**
     * Add one offer a buyer may choose for this slot.
     *
     * @param offer the offer reference
     * @return this builder
     */
    public FlexibleBundleSlotRequestBuilder offer(FlexibleBundleOfferRef offer) {
        this.offers = append(offers, offer);
        return this;
    }

    /**
     * Validate and build the slot request.
     *
     * @return the immutable slot request
     * @throws IllegalStateException if the order or required quantity is missing or
     *     out of range, or if the slot has no offers
     */
    public FlexibleBundleSlotRequest build() {
        if (order == null) {
            throw new IllegalStateException(ERR_ORDER_REQUIRED);
        }
        if (order < MIN_ORDER) {
            throw new IllegalStateException(ERR_ORDER_NEGATIVE);
        }
        if (requiredQuantity == null) {
            throw new IllegalStateException(ERR_QUANTITY_REQUIRED);
        }
        if (requiredQuantity < MIN_REQUIRED_QUANTITY) {
            throw new IllegalStateException(ERR_QUANTITY_MIN);
        }
        if (offers.isEmpty()) {
            throw new IllegalStateException(ERR_NO_OFFERS);
        }
        return new FlexibleBundleSlotRequest(id, order, entryPoint, requiredQuantity, offers);
    }

    private static List<FlexibleBundleOfferRef> append(
            List<FlexibleBundleOfferRef> current, FlexibleBundleOfferRef added) {
        List<FlexibleBundleOfferRef> next = new ArrayList<>(current);
        next.add(added);
        return List.copyOf(next);
    }
}
