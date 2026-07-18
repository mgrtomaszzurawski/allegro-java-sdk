/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

/**
 * Optional scope filter for {@code allegroPrices().streamOffersStatus(...)} —
 * narrows the query to offers in a particular Allegro Prices state.
 *
 * @since 0.2.0
 */
public enum OfferScope {

    /** Offers for which the seller has made a subsidy declaration. */
    WITH_DECLARATION,

    /** Offers currently discounted through Allegro Prices. */
    DISCOUNTED,

    /** Offers excluded from Allegro Prices. */
    EXCLUDED
}
