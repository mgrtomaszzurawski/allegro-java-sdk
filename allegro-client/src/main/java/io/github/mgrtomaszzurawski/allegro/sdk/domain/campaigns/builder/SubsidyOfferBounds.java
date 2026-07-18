/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

/**
 * The shared 1..1000 offer-count bound for the Allegro Prices submit and exclusion
 * command requests. Package-private — not part of the public surface.
 */
final class SubsidyOfferBounds {

    static final int MIN_OFFERS = 1;
    static final int MAX_OFFERS = 1000;

    private static final String ERR_COUNT =
            "offer count must be between " + MIN_OFFERS + " and " + MAX_OFFERS + ", got: ";

    private SubsidyOfferBounds() {
    }

    /**
     * @throws IllegalStateException if {@code count} is outside {@code [1, 1000]}
     */
    static void check(int count) {
        if (count < MIN_OFFERS || count > MAX_OFFERS) {
            throw new IllegalStateException(ERR_COUNT + count);
        }
    }
}
