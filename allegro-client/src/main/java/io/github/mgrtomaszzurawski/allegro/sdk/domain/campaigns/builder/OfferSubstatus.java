/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

/**
 * Optional sub-status filter for {@code allegroPrices().streamOffersStatus(...)}.
 *
 * @since 0.2.0
 */
public enum OfferSubstatus {

    /** Offers Allegro flags as having a discount opportunity. */
    DISCOUNT_OPPORTUNITY,

    /** Offers with an active discount recommendation. */
    DISCOUNT_RECOMMENDATION
}
