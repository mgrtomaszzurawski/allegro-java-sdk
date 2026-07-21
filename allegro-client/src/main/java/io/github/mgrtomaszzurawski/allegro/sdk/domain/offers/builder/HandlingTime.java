/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

/**
 * How long after a purchase the seller hands the parcel to the carrier — the
 * dispatch (handling) time Allegro accepts for a bulk modification. The wire
 * offers each value in two equivalent ISO 8601 spellings (an hours form and a
 * days form); the SDK exposes one clean set, so {@link #DAYS_2} maps to the
 * wire's {@code P2D}/{@code PT48H}.
 *
 * @since 0.5.0
 */
public enum HandlingTime {
    /** Dispatched immediately (same handling as the wire's {@code PT0S}). */
    IMMEDIATE,
    /** Dispatched within 1 day. */
    DAY_1,
    /** Dispatched within 2 days. */
    DAYS_2,
    /** Dispatched within 3 days. */
    DAYS_3,
    /** Dispatched within 4 days. */
    DAYS_4,
    /** Dispatched within 5 days. */
    DAYS_5,
    /** Dispatched within 7 days. */
    DAYS_7,
    /** Dispatched within 10 days. */
    DAYS_10,
    /** Dispatched within 14 days. */
    DAYS_14,
    /** Dispatched within 21 days. */
    DAYS_21,
    /** Dispatched within 30 days. */
    DAYS_30,
    /** Dispatched within 60 days. */
    DAYS_60
}
