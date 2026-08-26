/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormBuyerAddressReferenceRaw;
import org.jspecify.annotations.Nullable;

/**
 * The buyer's registered address on an order.
 *
 * @param street street and building/flat, or {@code null} when not provided
 * @param city city, or {@code null} when not provided
 * @param postCode postal code, or {@code null} when not provided
 * @param countryCode ISO country code, or {@code null} when not provided
 *
 * @since 0.8.0
 */
public record BuyerAddress(
        @Nullable String street,
        @Nullable String city,
        @Nullable String postCode,
        @Nullable String countryCode) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable BuyerAddress from(@Nullable CheckoutFormBuyerAddressReferenceRaw raw) {
        if (raw == null) {
            return null;
        }
        return new BuyerAddress(raw.getStreet(), raw.getCity(), raw.getPostCode(), raw.getCountryCode());
    }

    /**
     * Redacts the street and postal code so an accidental log or trace never leaks
     * the buyer's home address; use the typed accessors to read the fields
     * deliberately.
     */
    @Override
    public String toString() {
        return "BuyerAddress[city=" + city + ", countryCode=" + countryCode
                + ", personal data redacted]";
    }
}
