/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

/**
 * How long an offer stays listed — the set of listing durations Allegro accepts
 * for a bulk modification. The wire offers each duration in two equivalent ISO
 * 8601 spellings (an hours form and a days form); the SDK exposes one clean set,
 * so {@link #DAYS_3} maps to the wire's {@code P3D}/{@code PT72H}.
 *
 * <p>For an unlimited listing use {@code BatchModificationRequest.Builder.unlimitedListing()}
 * instead of a duration.
 *
 * @since 0.5.0
 */
public enum OfferDuration {
    /** Listed for 3 days. */
    DAYS_3,
    /** Listed for 5 days. */
    DAYS_5,
    /** Listed for 7 days. */
    DAYS_7,
    /** Listed for 10 days. */
    DAYS_10,
    /** Listed for 20 days. */
    DAYS_20,
    /** Listed for 30 days. */
    DAYS_30
}
