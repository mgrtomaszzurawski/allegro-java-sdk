/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceWithoutOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.Offers;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.UUID;

/**
 * Endpoint wrapper behind the {@link Offers} facade.
 *
 * @since 0.2.0
 */
public final class OffersImpl implements Offers {

    private static final String OP_GET = "get offer";
    private static final String OP_CHANGE_PRICE = "change offer Buy Now price";

    private final HttpSupport http;

    public OffersImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        // [append point: offers sub-facade wiring] Bucket A constructs its own
        // sub-facades here (batch/promoOptions/media); bucket F constructs its
        // sub-facades (tags/translations/bundles/flexibleBundles/rating) from
        // this same runtime. One block per bucket, append-only, BACKLOG order.
    }

    @Override
    public Offer get(String offerId) {
        return Offer.from(http.getAuthenticated(
                ApiPaths.productOffer(offerId), SaleProductOfferResponseV1Raw.class, OP_GET));
    }

    @Override
    public void changeBuyNowPrice(String offerId, Money buyNowPrice) {
        // Allegro's price change is a command keyed by a client-generated id;
        // for a single offer it resolves synchronously, so one PUT suffices.
        String commandId = UUID.randomUUID().toString();
        ChangePriceWithoutOutputRaw body = new ChangePriceWithoutOutputRaw()
                .id(commandId)
                .input(new ChangePriceInputRaw().buyNowPrice(
                        new PriceRaw().amount(buyNowPrice.amount()).currency(buyNowPrice.currency())));
        http.request(OP_CHANGE_PRICE)
                .put(ApiPaths.changePriceCommand(offerId, commandId))
                .jsonBody(body)
                .send();
    }
}
