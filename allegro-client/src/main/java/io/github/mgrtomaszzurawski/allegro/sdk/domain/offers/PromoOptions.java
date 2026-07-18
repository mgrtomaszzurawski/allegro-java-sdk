/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;

/**
 * Offer promotion packages (bold title, highlight, …) — reached via
 * {@code offers().promoOptions()}.
 *
 * @since 0.2.0
 */
public interface PromoOptions {

    /**
     * The promotion packages the seller can apply to offers.
     *
     * @return the available base and extra packages
     */
    AvailablePromotionPackages availablePackages();

    /**
     * The promotion packages currently applied to one offer.
     *
     * @param offerId the offer identifier
     * @return the offer's applied promotion packages
     */
    OfferPromoOptions forOffer(String offerId);
}
