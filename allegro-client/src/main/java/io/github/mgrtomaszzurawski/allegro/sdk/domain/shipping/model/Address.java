/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AddressRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.AddressBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Postal address of a point of service. When built for a create/update via
 * {@link #builder()}, {@code city}, {@code zipCode}, {@code state},
 * {@code countryCode} and {@code coordinates} are required (the live endpoint
 * rejects an address without coordinates — see {@code KNOWN-SERVER-BEHAVIORS.md});
 * as a read model, {@code coordinates} may be absent.
 *
 * @param street street and building number, or {@code null} when not set
 * @param city city (required)
 * @param zipCode postal code (required)
 * @param state region / voivodeship (required)
 * @param countryCode ISO 3166-1 alpha-2 country code (required)
 * @param coordinates geographic coordinates; required on write, may be
 *     {@code null} on a read
 *
 * @since 0.2.0
 */
public record Address(
        @Nullable String street,
        String city,
        String zipCode,
        String state,
        String countryCode,
        @Nullable Coordinates coordinates) {

    /** A fresh builder for an {@link Address}. */
    public static AddressBuilder builder() {
        return new AddressBuilder();
    }

    /** A builder pre-loaded with this address's fields. */
    public AddressBuilder toBuilder() {
        return new AddressBuilder()
                .street(street)
                .city(city)
                .zipCode(zipCode)
                .state(state)
                .countryCode(countryCode)
                .coordinates(coordinates);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static Address from(AddressRaw raw) {
        return new Address(
                raw.getStreet(),
                raw.getCity(),
                raw.getZipCode(),
                raw.getState(),
                raw.getCountryCode(),
                raw.getCoordinates() == null ? null : Coordinates.from(raw.getCoordinates()));
    }

    /** Build the generated Layer-1 DTO for a request body. */
    public AddressRaw toRaw() {
        AddressRaw raw = new AddressRaw();
        raw.setStreet(street);
        raw.setCity(city);
        raw.setZipCode(zipCode);
        raw.setState(state);
        raw.setCountryCode(countryCode);
        if (coordinates != null) {
            raw.setCoordinates(coordinates.toRaw());
        }
        return raw;
    }
}
