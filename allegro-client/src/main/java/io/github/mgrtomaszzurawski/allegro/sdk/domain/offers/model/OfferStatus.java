/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusRaw;
import org.jspecify.annotations.Nullable;

/**
 * Publication status of an offer.
 *
 * @since 0.2.0
 */
public enum OfferStatus {

    /** Draft or deactivated; not visible to buyers. */
    INACTIVE,
    /** Being published; not yet live. */
    ACTIVATING,
    /** Live and visible to buyers. */
    ACTIVE,
    /** No longer available. */
    ENDED,
    /** A status this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated publication status, tolerating unknown future values. */
    public static OfferStatus from(@Nullable OfferStatusRaw raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case INACTIVE -> INACTIVE;
            case ACTIVATING -> ACTIVATING;
            case ACTIVE -> ACTIVE;
            case ENDED -> ENDED;
            default -> UNKNOWN;
        };
    }
}
