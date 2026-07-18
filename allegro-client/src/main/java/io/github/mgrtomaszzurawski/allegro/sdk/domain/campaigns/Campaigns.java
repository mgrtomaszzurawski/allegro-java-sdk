/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;

/**
 * Marketing campaigns — reached via {@code AllegroClient.campaigns()}.
 *
 * <p>Groups Allegro's promotional programmes behind sub-facades. The starter
 * slice of bucket H ships {@link #badges()}; Allegro Prices and AlleDiscount
 * sub-accessors are added as the bucket lands, under the same append-only regime.
 *
 * <p>Every operation here requires the {@code allegro:api:campaigns} scope on a
 * seller user-context token.
 *
 * @since 0.2.0
 */
public interface Campaigns {

    /**
     * Badge campaigns: discover available campaigns, apply offers, and track
     * applications and active badges.
     *
     * @return the badge campaigns sub-facade
     */
    Badges badges();

    /**
     * Allegro Prices: manage the account's participation, read per-offer status,
     * and submit or exclude offers from the subsidy programme.
     *
     * @return the Allegro Prices sub-facade
     */
    AllegroPrices allegroPrices();

    /**
     * AlleDiscount: discover campaigns, read eligible and submitted offers, and
     * submit or withdraw an offer.
     *
     * @return the AlleDiscount sub-facade
     */
    AlleDiscount alleDiscount();
}
