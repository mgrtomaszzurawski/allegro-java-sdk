/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackagesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.PromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link PromoOptions} facade.
 *
 * @since 0.2.0
 */
public final class PromoOptionsImpl implements PromoOptions {

    private static final String OP_AVAILABLE = "get available promotion packages";
    private static final String OP_FOR_OFFER = "get offer promotion packages";

    private final HttpSupport http;

    public PromoOptionsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public AvailablePromotionPackages availablePackages() {
        return AvailablePromotionPackages.from(http.getAuthenticated(
                ApiPaths.OFFER_PROMOTION_PACKAGES, AvailablePromotionPackagesRaw.class, OP_AVAILABLE));
    }

    @Override
    public OfferPromoOptions forOffer(String offerId) {
        return OfferPromoOptions.from(http.getAuthenticated(
                ApiPaths.offerPromoOptions(offerId), OfferPromoOptionsRaw.class, OP_FOR_OFFER));
    }
}
