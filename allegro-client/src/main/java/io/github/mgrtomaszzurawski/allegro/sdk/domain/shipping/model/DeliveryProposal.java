/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryProposalDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Allegro's shipping suggestion for one order: a ready-to-submit shipment
 * ({@link #suggestedInput()}, which can be passed straight to
 * {@code shipping().createShipment(...)} or adjusted first) and the delivery
 * options available for that order. Read-only. Supersedes the deprecated
 * delivery-services resource.
 *
 * @param orderId the order this proposal is for, or {@code null}
 * @param suggestedInput a pre-filled shipment request for the order, or {@code null}
 * @param deliveryOptions the delivery options available for the order
 *
 * @since 0.5.0
 */
public record DeliveryProposal(
        @Nullable String orderId,
        @Nullable ShipmentRequest suggestedInput,
        List<DeliveryOption> deliveryOptions) {

    /** Canonical constructor: defensively copy the option list. */
    public DeliveryProposal {
        deliveryOptions = List.copyOf(deliveryOptions);
    }

    /** Map the generated response DTO to the public record. */
    public static DeliveryProposal from(DeliveryProposalDtoRaw raw) {
        return new DeliveryProposal(
                raw.getOrderId(),
                raw.getSuggestedInput() == null
                        ? null
                        : ShipmentRequest.from(raw.getSuggestedInput()),
                raw.getDeliveryOptions() == null
                        ? List.of()
                        : raw.getDeliveryOptions().stream().map(DeliveryOption::from).toList());
    }
}
