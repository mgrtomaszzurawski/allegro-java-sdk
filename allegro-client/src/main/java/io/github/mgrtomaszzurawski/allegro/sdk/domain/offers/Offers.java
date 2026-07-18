/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import java.util.stream.Stream;

/**
 * The offer lifecycle — reached via {@code AllegroClient.offers()} (bucket A).
 *
 * <p>Starter slice: a single-offer read ({@link #get(String)}) and a single-offer
 * price change ({@link #changeBuyNowPrice(String, Money)}). The rest of the
 * bucket — create/edit, bulk {@code batch()} commands, {@code promoOptions()},
 * {@code media()} — lands in the bucket's main package. Bucket F appends its
 * sub-accessors ({@code tags()}, {@code translations()}, {@code bundles()},
 * {@code flexibleBundles()}, {@code rating()}) to this same interface.
 *
 * @since 0.2.0
 */
public interface Offers {

    /**
     * Full data of a single product-offer.
     *
     * @param offerId the offer identifier
     * @return the offer
     */
    Offer get(String offerId);

    /**
     * Change an offer's Buy Now price. The SDK issues the price-change command
     * and returns once Allegro has accepted it.
     *
     * @param offerId     the offer identifier
     * @param buyNowPrice the new Buy Now price
     */
    void changeBuyNowPrice(String offerId, Money buyNowPrice);

    /**
     * Stream the seller's offers matching a filter, fetched page by page and
     * lazily — later pages are requested only as the stream is consumed.
     *
     * @param filter which offers to include (use {@link OfferFilter#all()} for all)
     * @return a lazy stream of offer summaries
     */
    Stream<OfferSummary> streamOffers(OfferFilter filter);

    /**
     * The Allegro Smart! classification report for one offer — whether it
     * qualifies and the per-condition breakdown.
     *
     * @param offerId the offer identifier
     * @return the Smart! classification report
     */
    SmartClassification smartClassification(String offerId);

    /**
     * Bulk offer operations (publish/unpublish in one Allegro batch command).
     *
     * @return the batch sub-facade
     */
    OfferBatch batch();

    // [append point: offers sub-facades] Bucket A appends its own sub-facade
    // accessors here (batch(), promoOptions(), media()); bucket F appends its
    // sub-accessors (tags(), translations(), bundles(), flexibleBundles(),
    // rating()). One block per bucket, append-only, in BACKLOG order.
}
