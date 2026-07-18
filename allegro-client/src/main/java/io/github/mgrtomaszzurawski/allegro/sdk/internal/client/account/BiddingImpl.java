/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.BidRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MaxPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MyBidResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.Bidding;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.MyBid;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link Bidding} facade.
 *
 * @since 0.2.0
 */
public final class BiddingImpl implements Bidding {

    private static final String OP_MY_BID = "get current bid";
    private static final String OP_PLACE_BID = "place bid";

    private final HttpSupport http;

    public BiddingImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public MyBid myBid(String offerId) {
        return MyBid.from(http.getAuthenticated(bidPath(offerId), MyBidResponseRaw.class, OP_MY_BID));
    }

    @Override
    public MyBid placeBid(String offerId, Money maxAmount) {
        BidRequestRaw request = new BidRequestRaw().maxAmount(
                new MaxPriceRaw().amount(maxAmount.amount()).currency(maxAmount.currency()));
        return MyBid.from(
                http.putJsonAuthenticated(bidPath(offerId), request, MyBidResponseRaw.class, OP_PLACE_BID));
    }

    private static String bidPath(String offerId) {
        return ApiPaths.subPath(ApiPaths.BIDDING_OFFERS, offerId, ApiPaths.BID_SEGMENT);
    }
}
