/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentOrderRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The parcels shipped from the fulfillment warehouse for a single order, read
 * via {@code fulfillment().parcelsOf(orderId)}.
 *
 * @param orderId the order these parcels belong to
 * @param parcels the shipped parcels (never {@code null}; empty when none)
 *
 * @since 0.3.0
 */
public record FulfillmentOrder(
        @Nullable String orderId,
        List<Parcel> parcels) {

    public FulfillmentOrder {
        parcels = parcels == null ? List.of() : List.copyOf(parcels);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static FulfillmentOrder from(FulfillmentOrderRaw raw) {
        return new FulfillmentOrder(
                raw.getOrderId(),
                raw.getParcels() == null ? List.of()
                        : raw.getParcels().stream().map(Parcel::from).toList());
    }
}
