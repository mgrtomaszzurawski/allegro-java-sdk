/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValuePublicationRaw;
import org.jspecify.annotations.Nullable;

/**
 * Whether — and how far along — an offer is published on an additional (foreign) marketplace.
 *
 * @since 0.6.0
 */
public enum MarketplacePublicationState {

    /** Publication on the marketplace was approved and is live. */
    APPROVED,
    /** Publication was refused (see the refusal reasons). */
    REFUSED,
    /** Publication is being processed. */
    IN_PROGRESS,
    /** Publication has not been requested for this marketplace. */
    NOT_REQUESTED,
    /** Publication is queued/pending. */
    PENDING,
    /** A state this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated state, tolerating unknown future values; {@code null} maps to {@code null}. */
    public static @Nullable MarketplacePublicationState from(
            AdditionalMarketplacesResponseValuePublicationRaw.@Nullable StateEnum raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case APPROVED -> APPROVED;
            case REFUSED -> REFUSED;
            case IN_PROGRESS -> IN_PROGRESS;
            case NOT_REQUESTED -> NOT_REQUESTED;
            case PENDING -> PENDING;
            default -> UNKNOWN;
        };
    }
}
