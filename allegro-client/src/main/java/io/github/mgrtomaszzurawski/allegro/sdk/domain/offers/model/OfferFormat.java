/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import org.jspecify.annotations.Nullable;

/**
 * How an offer is sold.
 *
 * @since 0.2.0
 */
public enum OfferFormat {

    /** Fixed Buy Now price. */
    BUY_NOW,
    /** Auction with bidding. */
    AUCTION,
    /** Advertisement (classified) listing. */
    ADVERTISEMENT,
    /** A format this SDK release does not model yet. */
    UNKNOWN;

    private static final String NOT_WRITABLE = "OfferFormat.UNKNOWN is a read-only sentinel and cannot be sent";

    /** Map the generated selling-mode format, tolerating unknown future values. */
    public static OfferFormat from(@Nullable SellingModeFormatRaw raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case BUY_NOW -> BUY_NOW;
            case AUCTION -> AUCTION;
            case ADVERTISEMENT -> ADVERTISEMENT;
            default -> UNKNOWN;
        };
    }

    /**
     * The generated selling-mode format for this value.
     *
     * @return the wire value
     * @throws IllegalStateException if called on {@link #UNKNOWN} (not writable)
     */
    public SellingModeFormatRaw toRaw() {
        return switch (this) {
            case BUY_NOW -> SellingModeFormatRaw.BUY_NOW;
            case AUCTION -> SellingModeFormatRaw.AUCTION;
            case ADVERTISEMENT -> SellingModeFormatRaw.ADVERTISEMENT;
            case UNKNOWN -> throw new IllegalStateException(NOT_WRITABLE);
        };
    }
}
