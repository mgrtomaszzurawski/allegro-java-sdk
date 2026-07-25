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

    private static final String ERR_NOT_SETTABLE =
            "status is not a value a client can request on publication: ";

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

    /**
     * Map to the generated status a client may request on an offer's publication:
     * {@link #ACTIVE} (publish/relist), {@link #INACTIVE} (keep as a draft / deactivate)
     * or {@link #ENDED} (end the offer). {@link #ACTIVATING} is a transient server-only
     * state and {@link #UNKNOWN} is not a real status, so neither is accepted.
     *
     * @throws IllegalArgumentException if the status cannot be requested by a client
     */
    public OfferStatusRaw toRaw() {
        return switch (this) {
            case INACTIVE -> OfferStatusRaw.INACTIVE;
            case ACTIVE -> OfferStatusRaw.ACTIVE;
            case ENDED -> OfferStatusRaw.ENDED;
            case ACTIVATING, UNKNOWN -> throw new IllegalArgumentException(ERR_NOT_SETTABLE + this);
        };
    }
}
