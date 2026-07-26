/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTags;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferPart;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferProcessingStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PartialOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceChangeResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.UnfilledParameters;
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
     * Selected parts of a single product-offer — a faster, lighter read than
     * {@link #get(String)} that returns only the requested {@link OfferPart parts}
     * (the available stock and/or the per-marketplace price). At least one part is
     * required.
     *
     * @param offerId the offer identifier
     * @param parts   the parts to retrieve (at least one)
     * @return the requested parts of the offer
     */
    PartialOffer getFields(String offerId, OfferPart... parts);

    /**
     * Change an offer's Buy Now price. A single-offer price change resolves
     * synchronously, so the SDK returns the command's terminal result — the price
     * it applied, the processing status, and any per-field errors.
     *
     * @param offerId     the offer identifier
     * @param buyNowPrice the new Buy Now price
     * @return the terminal result of the price-change command
     */
    PriceChangeResult changeBuyNowPrice(String offerId, Money buyNowPrice);

    /**
     * Create a new Buy Now offer. The offer is created as a draft; publish it
     * with {@code batch().publish(...)} once ready.
     *
     * @param request the offer to create
     * @return the created offer
     */
    Offer create(CreateOfferRequest request);

    /**
     * Apply a partial edit to an existing offer — only the fields set on the
     * request are changed; unset fields keep their current value.
     *
     * @param offerId the offer to edit
     * @param request the fields to change
     * @return the updated offer
     */
    Offer edit(String offerId, EditOfferRequest request);

    /**
     * Delete a draft (unpublished) offer.
     *
     * @param offerId the draft offer to delete
     */
    void deleteDraft(String offerId);

    /**
     * Stream the seller's offers matching a filter, fetched page by page and
     * lazily — later pages are requested only as the stream is consumed.
     *
     * @param filter which offers to include (use {@link OfferFilter#all()} for all)
     * @return a lazy stream of offer summaries
     */
    Stream<OfferSummary> streamOffers(OfferFilter filter);

    /**
     * A lazy stream of events about the seller's offers (activated, ended, price/stock
     * changed, …), newest resumable by event id. Filter by type with the given filter.
     *
     * @param filter which event types to include (use {@link OfferEventFilter#all()} for all)
     * @return a lazy stream of offer events
     */
    Stream<OfferEvent> streamEvents(OfferEventFilter filter);

    /**
     * The processing status of an asynchronous offer operation (the {@code operationId} an
     * async create/edit returns) — whether the background processing has finished.
     *
     * @param offerId     the offer the operation applies to
     * @param operationId the operation id to check
     * @return the processing status
     */
    OfferProcessingStatus operationStatus(String offerId, String operationId);

    /**
     * The Allegro Smart! classification report for one offer — whether it
     * qualifies and the per-condition breakdown.
     *
     * @param offerId the offer identifier
     * @return the Smart! classification report
     */
    SmartClassification smartClassification(String offerId);

    /**
     * Stream the seller's offers that are still missing category parameters,
     * fetched page by page and lazily.
     *
     * @return a lazy stream of offers with their unfilled parameter ids
     */
    Stream<UnfilledParameters> streamUnfilledParameters();

    /**
     * Bulk offer operations (publish/unpublish in one Allegro batch command).
     *
     * @return the batch sub-facade
     */
    OfferBatch batch();

    /**
     * Offer promotion packages (available packages and an offer's applied ones).
     *
     * @return the promo-options sub-facade
     */
    PromoOptions promoOptions();

    /**
     * Offer media — uploading offer images and document attachments.
     *
     * @return the media sub-facade
     */
    OfferMedia media();

    // [append point: offers sub-facades] Bucket A appends its own sub-facade
    // accessors here (batch(), promoOptions(), media()); bucket F appends its
    // sub-accessors (tags(), translations(), bundles(), flexibleBundles(),
    // rating()). One block per bucket, append-only, in BACKLOG order.

    // ---- bucket F sub-accessors ----
    /**
     * The seller's private offer tags and their assignment to offers.
     *
     * @return the offer-tags sub-facade
     */
    OfferTags tags();

    /**
     * An offer's translations into other languages.
     *
     * @return the offer-translations sub-facade
     */
    OfferTranslations translations();

    /**
     * The aggregated buyer rating of an offer.
     *
     * @param offerId the offer identifier
     * @return the offer's rating
     */
    OfferRating rating(String offerId);

    /**
     * The seller's fixed offer bundles.
     *
     * @return the offer-bundles sub-facade
     */
    OfferBundles bundles();

    /**
     * The seller's flexible offer bundles.
     *
     * @return the flexible-bundles sub-facade
     */
    FlexibleBundles flexibleBundles();
}
