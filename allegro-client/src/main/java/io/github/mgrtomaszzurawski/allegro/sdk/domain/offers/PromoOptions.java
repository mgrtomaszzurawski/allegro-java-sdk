/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoOptionModification;
import java.util.List;
import java.util.stream.Stream;

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
     * A lazy stream of the promotion packages currently applied across all of the seller's offers.
     *
     * @return a lazy stream of per-offer promotion packages
     */
    Stream<OfferPromoOptions> forAllOffers();

    /**
     * The promotion packages currently applied to one offer.
     *
     * @param offerId the offer identifier
     * @return the offer's applied promotion packages
     */
    OfferPromoOptions forOffer(String offerId);

    /**
     * Apply one or more promotion-package changes to a single offer (set/change a package, or
     * remove one now or at the end of its cycle).
     *
     * @param offerId the offer to change
     * @param changes the changes to apply (at least one)
     */
    void modify(String offerId, List<PromoOptionModification> changes);
}
