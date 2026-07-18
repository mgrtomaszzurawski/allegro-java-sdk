/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ExcludeOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ParticipationUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SubsidyCommandReport;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Allegro Prices — reached via {@code campaigns().allegroPrices()}. Manage the
 * account's participation, read per-offer Allegro Prices status, and submit or
 * exclude offers from the subsidy programme.
 *
 * <p>Every operation requires the {@code allegro:api:campaigns} scope on a seller
 * user-context token.
 *
 * @since 0.2.0
 */
public interface AllegroPrices {

    /**
     * Read the account's Allegro Prices participation across marketplaces.
     *
     * @return the current participation
     */
    AllegroPricesParticipation participation();

    /**
     * Update the account's Allegro Prices participation.
     *
     * @param update the per-marketplace participation change; see {@link ParticipationUpdate}
     * @return the participation after the update
     * @throws IllegalArgumentException if {@code update} is null
     */
    AllegroPricesParticipation updateParticipation(ParticipationUpdate update);

    /**
     * Stream the Allegro Prices status of the seller's offers, fetching pages
     * lazily as the stream is consumed.
     *
     * @param query the marketplace (required) and optional filters; see
     *     {@link AllegroPricesOfferQuery}
     * @return a lazy stream of offer statuses; never {@code null}
     * @throws IllegalArgumentException if {@code query} is null
     */
    Stream<AllegroPricesOfferStatus> streamOffersStatus(AllegroPricesOfferQuery query);

    /**
     * Submit offers to Allegro Prices, blocking until the command reaches a
     * terminal state (uses the default command timeout).
     *
     * @param request the offers to submit; see {@link SubmitOffersRequest}
     * @return the per-offer command report
     * @throws IllegalArgumentException if {@code request} is null
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within the timeout
     */
    SubsidyCommandReport submitOffers(SubmitOffersRequest request);

    /**
     * Submit offers to Allegro Prices, blocking until the command reaches a
     * terminal state or the given timeout elapses.
     *
     * @param request the offers to submit; see {@link SubmitOffersRequest}
     * @param timeout how long to wait for a terminal state
     * @return the per-offer command report
     * @throws IllegalArgumentException if {@code request} is null
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within {@code timeout}
     */
    SubsidyCommandReport submitOffers(SubmitOffersRequest request, Duration timeout);

    /**
     * Exclude offers from Allegro Prices, blocking until the command reaches a
     * terminal state (uses the default command timeout).
     *
     * @param request the offers to exclude; see {@link ExcludeOffersRequest}
     * @return the per-offer command report
     * @throws IllegalArgumentException if {@code request} is null
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within the timeout
     */
    SubsidyCommandReport excludeOffers(ExcludeOffersRequest request);

    /**
     * Exclude offers from Allegro Prices, blocking until the command reaches a
     * terminal state or the given timeout elapses.
     *
     * @param request the offers to exclude; see {@link ExcludeOffersRequest}
     * @param timeout how long to wait for a terminal state
     * @return the per-offer command report
     * @throws IllegalArgumentException if {@code request} is null
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within {@code timeout}
     */
    SubsidyCommandReport excludeOffers(ExcludeOffersRequest request, Duration timeout);
}
