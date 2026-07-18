/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryMethod;
import java.util.List;

/**
 * Shipping operations — reached via {@code AllegroClient.shipping()}: the
 * seller's delivery configuration and points of service. Shipment management
 * (WZA) lands in a later bucket-C PR.
 *
 * @since 0.2.0
 */
public interface Shipping {

    /**
     * List the delivery methods Allegro offers the seller. The response is not
     * paginated, so this returns a plain {@link List}.
     *
     * <p>Read-only and available with an application (client-credentials) token —
     * no user-context scope is required.
     *
     * @return the available delivery methods, possibly empty
     */
    List<DeliveryMethod> deliveryMethods();

    /**
     * Points of service — the seller's personal-collection locations.
     *
     * @return the points-of-service sub-facade
     */
    PointsOfService points();

    /**
     * Delivery settings — the seller's free-delivery thresholds and join policy.
     *
     * @return the delivery-settings sub-facade
     */
    DeliverySettings settings();

    /**
     * Shipping rates — the seller's per-delivery-method price sets.
     *
     * @return the shipping-rates sub-facade
     */
    ShippingRates rates();
}
