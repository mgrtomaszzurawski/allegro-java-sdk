/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryReferenceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * How an order is delivered: the method, the destination (a recipient address
 * or a pickup point), the delivery cost, the estimated time window, and whether
 * it is an Allegro Smart! delivery.
 *
 * <p>The delivery-cancellation details and the calculated number of packages are
 * not modelled; they are not seller-actionable. An order delivered to an address
 * has an {@code address} and no {@code pickupPoint}, and vice versa.
 *
 * @param method the chosen delivery method, or {@code null} when not set
 * @param address the recipient address, or {@code null} for a pickup-point delivery
 * @param pickupPoint the destination pickup point, or {@code null} for an address delivery
 * @param cost the delivery cost, or {@code null} when not set
 * @param time the estimated delivery-time window, or {@code null} when not set
 * @param smart {@code true} when this is an Allegro Smart! delivery; an absent
 *     wire value maps to {@code false}
 *
 * @since 0.7.0
 */
public record OrderDelivery(
        @Nullable DeliveryMethod method,
        @Nullable DeliveryAddress address,
        @Nullable DeliveryPickupPoint pickupPoint,
        @Nullable Money cost,
        @Nullable DeliveryTime time,
        boolean smart) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static OrderDelivery from(CheckoutFormDeliveryReferenceRaw raw) {
        var method = raw.getMethod();
        var address = raw.getAddress();
        var pickupPoint = raw.getPickupPoint();
        var time = raw.getTime();
        return new OrderDelivery(
                method == null ? null : DeliveryMethod.from(method),
                address == null ? null : DeliveryAddress.from(address),
                pickupPoint == null ? null : DeliveryPickupPoint.from(pickupPoint),
                Prices.money(raw.getCost()),
                time == null ? null : DeliveryTime.from(time),
                Boolean.TRUE.equals(raw.getSmart()));
    }
}
