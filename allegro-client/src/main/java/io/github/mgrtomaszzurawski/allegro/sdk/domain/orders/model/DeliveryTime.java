/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryTimeRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * The estimated delivery-time window for an order (the spec {@code from}/{@code to}
 * bounds, named here so they read clearly and satisfy the short-name rule).
 *
 * <p>The guaranteed-delivery and dispatch sub-windows are not modelled; the
 * estimated window is the seller-actionable value.
 *
 * @param earliestAt earliest estimated delivery time, or {@code null} when not set
 * @param latestAt latest estimated delivery time, or {@code null} when not set
 *
 * @since 0.7.0
 */
public record DeliveryTime(
        @Nullable OffsetDateTime earliestAt,
        @Nullable OffsetDateTime latestAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static DeliveryTime from(CheckoutFormDeliveryTimeRaw raw) {
        return new DeliveryTime(raw.getFrom(), raw.getTo());
    }
}
