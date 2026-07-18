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

    /** Map the generated selling-mode format, tolerating unknown future values. */
    public static OfferFormat from(@Nullable SellingModeFormatRaw raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case BUY_NOW -> BUY_NOW;
            case AUCTION -> AUCTION;
            case ADVERTISEMENT -> ADVERTISEMENT;
        };
    }
}
