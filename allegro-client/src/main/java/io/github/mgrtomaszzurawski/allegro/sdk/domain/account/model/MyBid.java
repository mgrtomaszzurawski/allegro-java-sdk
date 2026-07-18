/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CurrentPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MaxPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MyBidResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * The authenticated user's bid in an auction, as returned by
 * {@code Bidding.myBid(...)} and {@code Bidding.placeBid(...)}.
 *
 * @param maxAmount the maximum the user is willing to pay (proxy bid ceiling)
 * @param minimalPriceMet whether the auction's minimal price is met (or unset);
 *     {@code null} when the server omits it
 * @param highBidder whether this bid is currently winning
 * @param currentPrice the auction's current price
 *
 * @since 0.2.0
 */
public record MyBid(
        Money maxAmount,
        @Nullable Boolean minimalPriceMet,
        boolean highBidder,
        Money currentPrice) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static MyBid from(MyBidResponseRaw raw) {
        MaxPriceRaw maxPrice = raw.getMaxAmount();
        CurrentPriceRaw current = raw.getAuction().getCurrentPrice();
        return new MyBid(
                Money.of(maxPrice.getAmount(), maxPrice.getCurrency()),
                raw.getMinimalPriceMet(),
                raw.getHighBidder(),
                Money.of(current.getAmount(), current.getCurrency()));
    }
}
