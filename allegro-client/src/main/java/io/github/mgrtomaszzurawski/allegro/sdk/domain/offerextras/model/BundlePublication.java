/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BundlePublicationDTORaw;

/**
 * An offer bundle's publication status on one marketplace.
 *
 * @param marketplaceId identifier of the marketplace
 * @param status the bundle's status on that marketplace
 *
 * @since 0.2.0
 */
public record BundlePublication(String marketplaceId, BundlePublicationStatus status) {

    /** Map one generated Layer-1 publication DTO to the public record. */
    static BundlePublication from(BundlePublicationDTORaw raw) {
        return new BundlePublication(raw.getMarketplace().getId(), statusFrom(raw.getStatus()));
    }

    private static BundlePublicationStatus statusFrom(BundlePublicationDTORaw.StatusEnum raw) {
        try {
            return BundlePublicationStatus.valueOf(raw.name());
        } catch (IllegalArgumentException unknownStatus) {
            return BundlePublicationStatus.UNKNOWN;
        }
    }
}
