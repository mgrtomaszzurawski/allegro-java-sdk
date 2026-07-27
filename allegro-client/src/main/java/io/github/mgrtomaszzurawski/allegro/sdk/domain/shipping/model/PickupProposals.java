/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PickupProposalsResponseDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The carrier's pickup-time proposals for a set of shipments: the collection
 * address the proposals apply to and, per shipment, the available pickup
 * windows. Read-only. Feed a chosen {@link PickupTime} into a
 * {@code requestPickup(...)} call.
 *
 * @param address the collection address the proposals apply to, or {@code null}
 * @param proposals the per-shipment pickup-window proposals
 *
 * @since 0.5.0
 */
public record PickupProposals(
        @Nullable PostalAddress address,
        List<ShipmentPickupProposal> proposals) {

    /** Canonical constructor: defensively copy the list. */
    public PickupProposals {
        proposals = List.copyOf(proposals);
    }

    /** Map the generated response DTO to the public record. */
    public static PickupProposals from(PickupProposalsResponseDtoRaw raw) {
        return new PickupProposals(
                raw.getAddress() == null ? null : PostalAddress.fromPickup(raw.getAddress()),
                raw.getProposals() == null
                        ? List.of()
                        : raw.getProposals().stream().map(ShipmentPickupProposal::from).toList());
    }
}
