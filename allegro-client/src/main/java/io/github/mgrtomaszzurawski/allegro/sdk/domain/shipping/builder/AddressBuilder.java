/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Address;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Coordinates;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for an {@link Address}. {@code city}, {@code zipCode},
 * {@code state}, {@code countryCode} and {@code coordinates} are required (the
 * live points-of-service endpoint rejects an address without coordinates, though
 * the spec marks them optional — see {@code KNOWN-SERVER-BEHAVIORS.md});
 * {@code street}, {@code city} and {@code state} replicate the server length
 * limits.
 *
 * @since 0.2.0
 */
public final class AddressBuilder {

    private static final int MAX_STREET = 80;
    private static final int MAX_CITY = 40;
    private static final int MAX_ZIP_CODE = 10;
    private static final int MAX_STATE = 40;

    private static final String FIELD_CITY = "Address.city";
    private static final String FIELD_ZIP_CODE = "Address.zipCode";
    private static final String FIELD_STATE = "Address.state";
    private static final String FIELD_COUNTRY_CODE = "Address.countryCode";
    private static final String FIELD_STREET = "Address.street";
    private static final String FIELD_COORDINATES = "Address.coordinates";

    private @Nullable String street;
    private @Nullable String city;
    private @Nullable String zipCode;
    private @Nullable String state;
    private @Nullable String countryCode;
    private @Nullable Coordinates coordinates;

    /** Street and building number (optional, max 80 chars). */
    public AddressBuilder street(@Nullable String value) {
        this.street = value;
        return this;
    }

    /** City (required, max 40 chars). */
    public AddressBuilder city(@Nullable String value) {
        this.city = value;
        return this;
    }

    /** Postal code (required, max 10 chars). */
    public AddressBuilder zipCode(@Nullable String value) {
        this.zipCode = value;
        return this;
    }

    /** Region / voivodeship (required, max 40 chars). */
    public AddressBuilder state(@Nullable String value) {
        this.state = value;
        return this;
    }

    /** ISO 3166-1 alpha-2 country code (required). */
    public AddressBuilder countryCode(@Nullable String value) {
        this.countryCode = value;
        return this;
    }

    /** Geographic coordinates (required by the live points-of-service endpoint). */
    public AddressBuilder coordinates(@Nullable Coordinates value) {
        this.coordinates = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link Address}.
     *
     * @throws IllegalStateException if a required field is missing or a length
     *     limit is exceeded
     */
    public Address build() {
        String validCity = BuilderValidation.requireText(city, FIELD_CITY);
        String validZipCode = BuilderValidation.requireText(zipCode, FIELD_ZIP_CODE);
        String validState = BuilderValidation.requireText(state, FIELD_STATE);
        String validCountryCode = BuilderValidation.requireText(countryCode, FIELD_COUNTRY_CODE);
        BuilderValidation.requireMaxLength(street, MAX_STREET, FIELD_STREET);
        BuilderValidation.requireMaxLength(validCity, MAX_CITY, FIELD_CITY);
        BuilderValidation.requireMaxLength(validZipCode, MAX_ZIP_CODE, FIELD_ZIP_CODE);
        BuilderValidation.requireMaxLength(validState, MAX_STATE, FIELD_STATE);
        Coordinates validCoordinates = BuilderValidation.requirePresent(coordinates, FIELD_COORDINATES);
        return new Address(street, validCity, validZipCode, validState, validCountryCode,
                validCoordinates);
    }
}
