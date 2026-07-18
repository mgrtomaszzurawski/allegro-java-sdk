/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

/**
 * A statistical event type counted for a classifieds advertisement.
 *
 * @since 0.2.0
 */
public enum ClassifiedEventType {

    /** A viewer revealed the advertiser's phone number. */
    SHOWED_PHONE_NUMBER,

    /** A viewer sent a question about the advertisement. */
    ASKED_QUESTION,

    /** A viewer opened the ask-a-question form. */
    CLICKED_ASK_QUESTION,

    /** A viewer added the advertisement to favourites. */
    ADDED_TO_FAVOURITES,

    /** A viewer removed the advertisement from favourites. */
    REMOVED_FROM_FAVOURITES
}
