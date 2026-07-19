/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormDeliveryAddressRaw;
import org.jspecify.annotations.Nullable;

/**
 * The recipient's shipping address for an order delivered to an address (rather
 * than to a pickup point).
 *
 * <p>The address-modification timestamp is not modelled; it is not
 * seller-actionable.
 *
 * @param firstName recipient first name
 * @param lastName recipient last name
 * @param street street and building/flat
 * @param city city
 * @param zipCode postal code
 * @param countryCode ISO country code
 * @param companyName company name, or {@code null} for a private recipient
 * @param phoneNumber contact phone number, or {@code null} when not provided
 *
 * @since 0.7.0
 */
public record DeliveryAddress(
        String firstName,
        String lastName,
        String street,
        String city,
        String zipCode,
        String countryCode,
        @Nullable String companyName,
        @Nullable String phoneNumber) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static DeliveryAddress from(CheckoutFormDeliveryAddressRaw raw) {
        return new DeliveryAddress(
                raw.getFirstName(),
                raw.getLastName(),
                raw.getStreet(),
                raw.getCity(),
                raw.getZipCode(),
                raw.getCountryCode(),
                raw.getCompanyName(),
                raw.getPhoneNumber());
    }

    /**
     * Redacts the recipient's personal data (name, street, phone) so an
     * accidental log or trace of a {@code DeliveryAddress} never leaks it; use
     * the typed accessors to read the fields deliberately.
     */
    @Override
    public String toString() {
        return "DeliveryAddress[city=" + city + ", countryCode=" + countryCode
                + ", personal data redacted]";
    }
}
