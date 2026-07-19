/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackagesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsForSellerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsModificationsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.PromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoOptionModification;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link PromoOptions} facade.
 *
 * @since 0.2.0
 */
public final class PromoOptionsImpl implements PromoOptions {

    private static final String OP_AVAILABLE = "get available promotion packages";
    private static final String OP_FOR_ALL = "stream all offers' promotion packages";
    private static final String OP_FOR_OFFER = "get offer promotion packages";
    private static final String OP_MODIFY = "modify offer promotion packages";

    /** Offers page ≤ 1000 (spec); 100 balances round-trips against payload size. */
    private static final int PAGE_SIZE = 100;
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

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
    public Stream<OfferPromoOptions> forAllOffers() {
        return PagedSpliterator.stream(this::fetchPage);
    }

    private PagedSpliterator.Page<OfferPromoOptions> fetchPage(int pageIndex) {
        Query query = Query.create()
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        OfferPromoOptionsForSellerRaw response = http.request(OP_FOR_ALL)
                .get(ApiPaths.SALE_OFFERS_PROMO_OPTIONS)
                .query(query)
                .fetch(OfferPromoOptionsForSellerRaw.class);
        List<OfferPromoOptionsRaw> promoOptions = response.getPromoOptions();
        List<OfferPromoOptions> items = promoOptions == null
                ? List.of()
                : promoOptions.stream().map(OfferPromoOptions::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public OfferPromoOptions forOffer(String offerId) {
        return OfferPromoOptions.from(http.getAuthenticated(
                ApiPaths.offerPromoOptions(offerId), OfferPromoOptionsRaw.class, OP_FOR_OFFER));
    }

    @Override
    public void modify(String offerId, List<PromoOptionModification> changes) {
        PromoOptionsModificationsRaw body = new PromoOptionsModificationsRaw()
                .modifications(changes.stream().map(PromoOptionModification::toRaw).toList());
        http.request(OP_MODIFY)
                .post(ApiPaths.offerPromoOptionsModification(offerId))
                .jsonBody(body)
                .send();
    }
}
