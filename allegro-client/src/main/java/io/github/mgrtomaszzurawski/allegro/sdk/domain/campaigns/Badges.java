/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgePatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.Badge;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperation;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * Badge campaigns — reached via {@code campaigns().badges()}. Discover the
 * campaigns offered to the seller, apply badges to offers, track the resulting
 * applications and active badges, and update or finish a badge.
 *
 * <p>Every operation requires the {@code allegro:api:campaigns} scope on a seller
 * user-context token.
 *
 * @since 0.2.0
 */
public interface Badges {

    /**
     * Badge campaigns currently offered to the authenticated seller, across every
     * marketplace the account sells on.
     *
     * @return the available campaigns; never {@code null}, possibly empty
     */
    List<BadgeCampaign> availableCampaigns();

    /**
     * Badge campaigns offered to the authenticated seller on a single marketplace.
     *
     * @param marketplaceId marketplace id (e.g. {@code "allegro-pl"}); must not be null or blank
     * @return the available campaigns on that marketplace; never {@code null}, possibly empty
     * @throws IllegalArgumentException if {@code marketplaceId} is null or blank — use
     *     {@link #availableCampaigns()} for the all-marketplaces listing
     */
    List<BadgeCampaign> availableCampaigns(String marketplaceId);

    /**
     * Apply a campaign badge to one of the seller's offers.
     *
     * <p>Allegro verifies badge applications asynchronously, often with an
     * e-mail-notified manual step that can take a long time — so this call does
     * <strong>not</strong> block to a terminal state. It returns the created
     * application, typically {@link io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplicationStatus#REQUESTED};
     * track its outcome with {@link #application(String)} or {@link #streamApplications(BadgeApplicationFilter)}.
     *
     * @param request the application to submit; see {@link BadgeApplicationRequest}
     * @return the created application
     */
    BadgeApplication apply(BadgeApplicationRequest request);

    /**
     * Stream the seller's badge applications, most recent first, fetching pages
     * lazily as the stream is consumed.
     *
     * @param filter narrows the applications by campaign and/or offer; use
     *     {@link BadgeApplicationFilter#all()} for every application
     * @return a lazy stream of applications; never {@code null}
     */
    Stream<BadgeApplication> streamApplications(BadgeApplicationFilter filter);

    /**
     * Read a single badge application by id.
     *
     * @param applicationId the application id; must not be null or blank
     * @return the application
     * @throws IllegalArgumentException if {@code applicationId} is null or blank
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException
     *     if no application has that id
     */
    BadgeApplication application(String applicationId);

    /**
     * Stream the seller's active badges on a marketplace, fetching pages lazily as
     * the stream is consumed.
     *
     * @param filter the marketplace (required) and optional offer to stream badges
     *     for; see {@link BadgeFilter}
     * @return a lazy stream of badges; never {@code null}
     */
    Stream<Badge> streamBadges(BadgeFilter filter);

    /**
     * Update or finish an offer's active badge, blocking until the change reaches a
     * terminal state (uses the default command timeout).
     *
     * @param offerId    the offer whose badge changes; must not be null or blank
     * @param campaignId the badge campaign; must not be null or blank
     * @param patch      the change to apply; see {@link BadgePatch}
     * @return the terminal operation outcome
     * @throws IllegalArgumentException if {@code offerId} or {@code campaignId} is null or blank
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the operation does not finish within the timeout
     */
    BadgeOperation update(String offerId, String campaignId, BadgePatch patch);

    /**
     * Update or finish an offer's active badge, blocking until the change reaches a
     * terminal state or the given timeout elapses.
     *
     * @param offerId    the offer whose badge changes; must not be null or blank
     * @param campaignId the badge campaign; must not be null or blank
     * @param patch      the change to apply; see {@link BadgePatch}
     * @param timeout    how long to wait for a terminal state
     * @return the terminal operation outcome
     * @throws IllegalArgumentException if {@code offerId} or {@code campaignId} is null or blank
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the operation does not finish within {@code timeout}
     */
    BadgeOperation update(String offerId, String campaignId, BadgePatch patch, Duration timeout);
}
