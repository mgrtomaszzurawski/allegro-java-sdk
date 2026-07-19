/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryMethodRaw;
import org.jspecify.annotations.Nullable;

/**
 * The delivery method chosen for an order.
 *
 * @param id delivery-method identifier, or {@code null} when not set
 * @param name human-readable method name, or {@code null} when not set
 *
 * @since 0.7.0
 */
public record DeliveryMethod(@Nullable String id, @Nullable String name) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static DeliveryMethod from(CheckoutFormDeliveryMethodRaw raw) {
        return new DeliveryMethod(raw.getId(), raw.getName());
    }
}
