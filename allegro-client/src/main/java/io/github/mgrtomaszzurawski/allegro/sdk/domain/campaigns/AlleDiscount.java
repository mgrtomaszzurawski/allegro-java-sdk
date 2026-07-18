/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.EligibleOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmittedOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountEligibleOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmitResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmittedOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountWithdrawResult;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * AlleDiscount — reached via {@code campaigns().alleDiscount()}. Discover the
 * campaigns, read eligible and submitted offers, and submit or withdraw an offer.
 *
 * <p>Every operation requires the {@code allegro:api:campaigns} scope on a seller
 * user-context token.
 *
 * @since 0.2.0
 */
public interface AlleDiscount {

    /**
     * List the AlleDiscount campaigns available to the seller.
     *
     * @return the campaigns; never {@code null}, possibly empty (the endpoint is not paginated)
     */
    List<AlleDiscountCampaign> campaigns();

    /**
     * Stream a campaign's offers that are eligible for AlleDiscount, fetching pages
     * lazily as the stream is consumed.
     *
     * @param filter the campaign (required) and optional filters; see {@link EligibleOffersFilter}
     * @return a lazy stream of eligible offers; never {@code null}
     * @throws IllegalArgumentException if {@code filter} is null
     */
    Stream<AlleDiscountEligibleOffer> streamEligibleOffers(EligibleOffersFilter filter);

    /**
     * Stream the offers the seller has submitted to a campaign, fetching pages
     * lazily as the stream is consumed.
     *
     * @param filter the campaign (required) and optional filters; see {@link SubmittedOffersFilter}
     * @return a lazy stream of submitted offers; never {@code null}
     * @throws IllegalArgumentException if {@code filter} is null
     */
    Stream<AlleDiscountSubmittedOffer> streamSubmittedOffers(SubmittedOffersFilter filter);

    /**
     * Submit an offer to an AlleDiscount campaign, blocking until the command
     * reaches a terminal state (uses the default command timeout).
     *
     * @param request the submission; see {@link SubmitOfferRequest}
     * @return the terminal command result
     * @throws IllegalArgumentException if {@code request} is null
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within the timeout
     */
    AlleDiscountSubmitResult submitOffer(SubmitOfferRequest request);

    /**
     * Submit an offer to an AlleDiscount campaign, blocking until the command
     * reaches a terminal state or the given timeout elapses.
     *
     * @param request the submission; see {@link SubmitOfferRequest}
     * @param timeout how long to wait for a terminal state
     * @return the terminal command result
     * @throws IllegalArgumentException if {@code request} is null
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within {@code timeout}
     */
    AlleDiscountSubmitResult submitOffer(SubmitOfferRequest request, Duration timeout);

    /**
     * Withdraw a submitted offer from AlleDiscount, blocking until the command
     * reaches a terminal state (uses the default command timeout).
     *
     * @param participationId the participation to withdraw (from a submitted offer);
     *     must not be null or blank
     * @return the terminal command result
     * @throws IllegalArgumentException if {@code participationId} is null or blank
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within the timeout
     */
    AlleDiscountWithdrawResult withdrawOffer(String participationId);

    /**
     * Withdraw a submitted offer from AlleDiscount, blocking until the command
     * reaches a terminal state or the given timeout elapses.
     *
     * @param participationId the participation to withdraw; must not be null or blank
     * @param timeout         how long to wait for a terminal state
     * @return the terminal command result
     * @throws IllegalArgumentException if {@code participationId} is null or blank
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within {@code timeout}
     */
    AlleDiscountWithdrawResult withdrawOffer(String participationId, Duration timeout);
}
