/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryPickupPointAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryPickupPointRaw;
import org.jspecify.annotations.Nullable;

/**
 * The pickup point an order is delivered to, when the buyer chose delivery to a
 * point rather than to an address. The point's location fields are flattened
 * onto this record.
 *
 * @param id pickup-point identifier, or {@code null} when not set
 * @param name pickup-point name, or {@code null} when not set
 * @param description pickup-point description, or {@code null} when not set
 * @param street point street address, or {@code null} when not set
 * @param zipCode point postal code, or {@code null} when not set
 * @param city point city, or {@code null} when not set
 * @param countryCode point ISO country code, or {@code null} when not set
 *
 * @since 0.7.0
 */
public record DeliveryPickupPoint(
        @Nullable String id,
        @Nullable String name,
        @Nullable String description,
        @Nullable String street,
        @Nullable String zipCode,
        @Nullable String city,
        @Nullable String countryCode) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static DeliveryPickupPoint from(CheckoutFormDeliveryPickupPointRaw raw) {
        CheckoutFormDeliveryPickupPointAddressRaw address = raw.getAddress();
        return new DeliveryPickupPoint(
                raw.getId(),
                raw.getName(),
                raw.getDescription(),
                address == null ? null : address.getStreet(),
                address == null ? null : address.getZipCode(),
                address == null ? null : address.getCity(),
                address == null ? null : address.getCountryCode());
    }
}
