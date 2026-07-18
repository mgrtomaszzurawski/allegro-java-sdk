/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OrdersShippingCarrierRaw;
import org.jspecify.annotations.Nullable;

/**
 * A shipping carrier the seller may reference when adding a parcel tracking
 * number to an order (from the {@code orders().carriers()} dictionary).
 *
 * @param id carrier identifier to pass on a shipment (e.g. {@code UPS})
 * @param name human-readable carrier name, or {@code null} when absent
 *
 * @since 0.4.0
 */
public record Carrier(String id, @Nullable String name) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static Carrier from(OrdersShippingCarrierRaw raw) {
        return new Carrier(raw.getId(), raw.getName());
    }
}
