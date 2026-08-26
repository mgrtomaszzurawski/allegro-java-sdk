/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryTimeDispatchRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryTimeGuaranteedRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A {@code from}/{@code to} time window inside an order's {@link DeliveryTime} —
 * either the guaranteed-delivery window or the dispatch window.
 *
 * @param earliestAt window start (wire {@code from}), or {@code null} when not set
 * @param latestAt window end (wire {@code to}), or {@code null} when not set
 *
 * @since 0.8.0
 */
public record DeliveryTimeWindow(
        @Nullable OffsetDateTime earliestAt,
        @Nullable OffsetDateTime latestAt) {

    /** Map the generated guaranteed-delivery DTO, or {@code null} when absent. */
    public static @Nullable DeliveryTimeWindow from(@Nullable CheckoutFormDeliveryTimeGuaranteedRaw raw) {
        if (raw == null) {
            return null;
        }
        return new DeliveryTimeWindow(raw.getFrom(), raw.getTo());
    }

    /** Map the generated dispatch DTO, or {@code null} when absent. */
    public static @Nullable DeliveryTimeWindow from(@Nullable CheckoutFormDeliveryTimeDispatchRaw raw) {
        if (raw == null) {
            return null;
        }
        return new DeliveryTimeWindow(raw.getFrom(), raw.getTo());
    }
}
