/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

/**
 * Shipping operations — reached via {@code AllegroClient.shipping()}.
 *
 * <p>Starter slice of bucket C (shipping): only the {@link #points()}
 * sub-facade ships in this PR. Shipment management, delivery configuration and
 * shipping rates are added in the bucket's volume PR.
 *
 * @since 0.2.0
 */
public interface Shipping {

    /**
     * Points of service — the seller's personal-collection locations.
     *
     * @return the points-of-service sub-facade
     */
    PointsOfService points();
}
