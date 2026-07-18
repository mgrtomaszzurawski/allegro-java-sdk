/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import java.util.Objects;

/**
 * A reference to one offer a buyer may choose for a flexible-bundle slot, plus
 * whether that offer is excluded from the bundle's discount. Build one with
 * {@link #of(String, boolean)}.
 *
 * @param offerId the offer identifier
 * @param excludedFromDiscount whether this offer is excluded from the bundle
 *     discount when a buyer picks it for the slot
 *
 * @since 0.2.0
 */
public record FlexibleBundleOfferRef(String offerId, boolean excludedFromDiscount) {

    private static final String ERR_OFFER_ID_NULL = "offerId must not be null";

    public FlexibleBundleOfferRef {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
    }

    /**
     * An offer reference for a slot.
     *
     * @param offerId the offer identifier
     * @param excludedFromDiscount whether the offer is excluded from the discount
     * @return an offer reference
     */
    public static FlexibleBundleOfferRef of(String offerId, boolean excludedFromDiscount) {
        return new FlexibleBundleOfferRef(offerId, excludedFromDiscount);
    }
}
