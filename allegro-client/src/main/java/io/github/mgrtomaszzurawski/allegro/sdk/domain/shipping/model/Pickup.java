/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PickupDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A carrier pickup booked for one or more shipments: the SDK's pickup id, the
 * carrier's own pickup id, the carrier, the collection address and the shipments
 * and waybills it covers. Read-only.
 *
 * @param id the pickup id, or {@code null}
 * @param carrierPickupId the carrier's own pickup identifier, or {@code null}
 * @param carrier the carrier code, or {@code null}
 * @param pickupCarrier the pickup carrier code, or {@code null}
 * @param address the collection address, or {@code null}
 * @param shipmentIds the shipments this pickup collects
 * @param waybills the waybill numbers collected
 *
 * @since 0.5.0
 */
public record Pickup(
        @Nullable String id,
        @Nullable String carrierPickupId,
        @Nullable String carrier,
        @Nullable String pickupCarrier,
        @Nullable PostalAddress address,
        List<String> shipmentIds,
        List<String> waybills) {

    /** Canonical constructor: defensively copy the lists. */
    public Pickup {
        shipmentIds = List.copyOf(shipmentIds);
        waybills = List.copyOf(waybills);
    }

    /** Map the generated response DTO to the public record. */
    public static Pickup from(PickupDtoRaw raw) {
        return new Pickup(
                raw.getId(),
                raw.getCarrierPickupId(),
                raw.getCarrier(),
                raw.getPickupCarrier(),
                raw.getAddress() == null ? null : PostalAddress.fromSender(raw.getAddress()),
                raw.getShipmentIds() == null ? List.of() : List.copyOf(raw.getShipmentIds()),
                raw.getWaybills() == null ? List.of() : List.copyOf(raw.getWaybills()));
    }
}
