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
 * <p>Alongside the estimated window, the {@link #guaranteed()} delivery window and
 * the {@link #dispatch()} window are exposed when the order carries them.
 *
 * @param earliestAt earliest estimated delivery time, or {@code null} when not set
 * @param latestAt latest estimated delivery time, or {@code null} when not set
 * @param guaranteed the guaranteed-delivery window, or {@code null} when not set
 * @param dispatch the dispatch window, or {@code null} when not set
 *
 * @since 0.7.0
 */
public record DeliveryTime(
        @Nullable OffsetDateTime earliestAt,
        @Nullable OffsetDateTime latestAt,
        @Nullable DeliveryTimeWindow guaranteed,
        @Nullable DeliveryTimeWindow dispatch) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static DeliveryTime from(CheckoutFormDeliveryTimeRaw raw) {
        return new DeliveryTime(
                raw.getFrom(),
                raw.getTo(),
                DeliveryTimeWindow.from(raw.getGuaranteed()),
                DeliveryTimeWindow.from(raw.getDispatch()));
    }
}
