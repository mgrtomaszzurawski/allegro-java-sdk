/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryMethod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Shipment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import java.time.Duration;
import java.util.List;

/**
 * Shipping operations — reached via {@code AllegroClient.shipping()}: the
 * seller's delivery configuration, points of service, and carrier shipment
 * management ("Wysyłam z Allegro").
 *
 * @since 0.2.0
 */
public interface Shipping {

    /**
     * Create a carrier shipment and wait for it to be ready. The create endpoint
     * is asynchronous — the SDK submits the command and polls until it reaches a
     * terminal state, then reads the created shipment back, so this call is
     * synchronous with no command handle exposed.
     *
     * @param request the shipment to create
     * @return the created shipment
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within the default timeout
     */
    Shipment createShipment(ShipmentRequest request);

    /**
     * Create a carrier shipment, waiting at most {@code timeout} for it.
     *
     * @param request the shipment to create
     * @param timeout the overall budget to wait for the command to finish
     * @return the created shipment
     * @see #createShipment(ShipmentRequest)
     */
    Shipment createShipment(ShipmentRequest request, Duration timeout);

    /**
     * Read a shipment by id.
     *
     * @param shipmentId the shipment id
     * @return the shipment
     */
    Shipment getShipment(String shipmentId);

    /**
     * Cancel a shipment and wait for the cancellation to complete. Like creation,
     * the cancel endpoint is asynchronous and is polled to a terminal state.
     *
     * @param shipmentId the shipment to cancel
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException
     *     if the command does not finish within the default timeout
     */
    void cancelShipment(String shipmentId);

    /**
     * Cancel a shipment, waiting at most {@code timeout} for it.
     *
     * @param shipmentId the shipment to cancel
     * @param timeout the overall budget to wait for the command to finish
     * @see #cancelShipment(String)
     */
    void cancelShipment(String shipmentId, Duration timeout);

    /**
     * Render the shipping labels for one or more shipments.
     *
     * @param request which shipments to render and how
     * @return the rendered labels as raw bytes (PDF or ZPL per the shipments' format)
     */
    byte[] labels(LabelRequest request);

    /**
     * Render the carrier handover protocol for one or more shipments.
     *
     * @param shipmentIds the shipments to include in the protocol (at least one)
     * @return the rendered protocol as raw bytes (PDF)
     */
    byte[] protocol(String... shipmentIds);

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
