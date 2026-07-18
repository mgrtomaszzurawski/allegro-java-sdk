/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BundledOfferDTORaw;

/**
 * One offer that is part of an offer bundle.
 *
 * @param offerId the bundled offer's identifier
 * @param requiredQuantity how many units of this offer the bundle requires
 * @param entryPoint whether this offer is the bundle's entry point (the offer a
 *     buyer starts from)
 *
 * @since 0.2.0
 */
public record BundledOffer(String offerId, int requiredQuantity, boolean entryPoint) {

    /** Map the generated Layer-1 DTO to the public record. */
    static BundledOffer from(BundledOfferDTORaw raw) {
        return new BundledOffer(raw.getId(), raw.getRequiredQuantity(), raw.getEntryPoint());
    }
}
