/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PickupProposalsRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.PickupProposalsRequestBuilder;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A request for the carrier's available pickup windows: which shipments to
 * collect, the collection address and, optionally, the date they will be ready.
 *
 * @param shipmentIds the shipments to collect (at least one)
 * @param readyDate the date the shipments are ready (ISO {@code yyyy-MM-dd}), or {@code null}
 * @param address the collection address
 *
 * @since 0.5.0
 */
public record PickupProposalsRequest(
        List<String> shipmentIds,
        @Nullable String readyDate,
        PostalAddress address) {

    /** Canonical constructor: defensively copy the shipment-id list. */
    public PickupProposalsRequest {
        shipmentIds = List.copyOf(shipmentIds);
    }

    /** A fresh builder for a {@link PickupProposalsRequest}. */
    public static PickupProposalsRequestBuilder builder() {
        return new PickupProposalsRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public PickupProposalsRequestBuilder toBuilder() {
        return new PickupProposalsRequestBuilder()
                .shipmentIds(shipmentIds)
                .readyDate(readyDate)
                .address(address);
    }

    /** Build the generated Layer-1 DTO for the request body. */
    public PickupProposalsRequestDtoRaw toRaw() {
        PickupProposalsRequestDtoRaw raw = new PickupProposalsRequestDtoRaw();
        raw.setShipmentIds(List.copyOf(shipmentIds));
        raw.setReadyDate(readyDate);
        raw.setAddress(address.toPickupRaw());
        return raw;
    }
}
