/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.MyBid;

/**
 * Auction bidding (buyer side) — reached via {@code AllegroClient.bidding()}.
 *
 * <p>Works with a <strong>buyer</strong> user-context token (the {@code bids}
 * scope); the same SDK serves both sides of the marketplace.
 *
 * @since 0.2.0
 */
public interface Bidding {

    /**
     * The authenticated user's current bid in an auction.
     *
     * @param offerId the auction offer id
     * @return the user's bid information
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException
     *     if the auction does not exist or the user has not bid in it (the
     *     Allegro API returns 404 for both cases)
     */
    MyBid myBid(String offerId);

    /**
     * Place (or raise) a proxy bid in an auction.
     *
     * @param offerId the auction offer id
     * @param maxAmount the maximum the user is willing to pay
     * @return the resulting bid information
     */
    MyBid placeBid(String offerId, Money maxAmount);
}
