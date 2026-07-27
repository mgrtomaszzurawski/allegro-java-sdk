/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PickupDateProposalsDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The pickup windows a carrier offers for one shipment: the shipment id and the
 * available {@link PickupTime} windows to choose from when requesting a pickup.
 * Read-only. (The spec's older proposal-id items are deprecated and not exposed;
 * a request uses a {@link PickupTime} window directly.)
 *
 * @param shipmentId the shipment these windows are for, or {@code null}
 * @param pickupTimes the pickup windows on offer
 *
 * @since 0.5.0
 */
public record ShipmentPickupProposal(
        @Nullable String shipmentId,
        List<PickupTime> pickupTimes) {

    /** Canonical constructor: defensively copy the list. */
    public ShipmentPickupProposal {
        pickupTimes = List.copyOf(pickupTimes);
    }

    /** Map the generated DTO to the public record (deprecated proposal items dropped). */
    public static ShipmentPickupProposal from(PickupDateProposalsDtoRaw raw) {
        return new ShipmentPickupProposal(
                raw.getShipmentId(),
                raw.getPickupTimes() == null
                        ? List.of()
                        : raw.getPickupTimes().stream().map(PickupTime::from).toList());
    }
}
