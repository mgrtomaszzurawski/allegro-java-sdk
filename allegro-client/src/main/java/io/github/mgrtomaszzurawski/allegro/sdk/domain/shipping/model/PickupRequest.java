/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PickupCreateRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.PickupRequestBuilder;
import java.util.List;

/**
 * A request to book a carrier pickup: which shipments to collect, the pickup
 * time window and the collection address — all required. The window is chosen
 * from a {@link PickupProposals} result or supplied directly.
 *
 * @param shipmentIds the shipments to collect (at least one)
 * @param pickupTime the requested pickup window
 * @param address the collection address
 *
 * @since 0.5.0
 */
public record PickupRequest(
        List<String> shipmentIds,
        PickupTime pickupTime,
        PostalAddress address) {

    /** Canonical constructor: defensively copy the shipment-id list. */
    public PickupRequest {
        shipmentIds = List.copyOf(shipmentIds);
    }

    /** A fresh builder for a {@link PickupRequest}. */
    public static PickupRequestBuilder builder() {
        return new PickupRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public PickupRequestBuilder toBuilder() {
        return new PickupRequestBuilder()
                .shipmentIds(shipmentIds)
                .pickupTime(pickupTime)
                .address(address);
    }

    /** Build the generated Layer-1 DTO for the create-command request body. */
    public PickupCreateRequestDtoRaw toRaw() {
        PickupCreateRequestDtoRaw raw = new PickupCreateRequestDtoRaw();
        raw.setShipmentIds(List.copyOf(shipmentIds));
        raw.setPickupTime(pickupTime.toRaw());
        raw.setAddress(address.toPickupRaw());
        return raw;
    }
}
