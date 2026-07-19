/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link PostalAddress}. The street, postal code, city,
 * e-mail and phone are required; the name, company, state and pickup-point id
 * are optional.
 *
 * @since 0.4.0
 */
public final class PostalAddressBuilder {

    private static final String FIELD_STREET = "PostalAddress.street";
    private static final String FIELD_POSTAL_CODE = "PostalAddress.postalCode";
    private static final String FIELD_CITY = "PostalAddress.city";
    private static final String FIELD_EMAIL = "PostalAddress.email";
    private static final String FIELD_PHONE = "PostalAddress.phone";

    private @Nullable String name;
    private @Nullable String company;
    private @Nullable String street;
    private @Nullable String postalCode;
    private @Nullable String city;
    private @Nullable String state;
    private @Nullable String email;
    private @Nullable String phone;
    private @Nullable String point;

    /** The addressee's name (optional). */
    public PostalAddressBuilder name(@Nullable String value) {
        this.name = value;
        return this;
    }

    /** The company name (optional). */
    public PostalAddressBuilder company(@Nullable String value) {
        this.company = value;
        return this;
    }

    /** The street and building/flat number (required). */
    public PostalAddressBuilder street(@Nullable String value) {
        this.street = value;
        return this;
    }

    /** The postal code (required). */
    public PostalAddressBuilder postalCode(@Nullable String value) {
        this.postalCode = value;
        return this;
    }

    /** The city (required). */
    public PostalAddressBuilder city(@Nullable String value) {
        this.city = value;
        return this;
    }

    /** The region/voivodeship (optional). */
    public PostalAddressBuilder state(@Nullable String value) {
        this.state = value;
        return this;
    }

    /** The contact e-mail (required). */
    public PostalAddressBuilder email(@Nullable String value) {
        this.email = value;
        return this;
    }

    /** The contact phone number (required). */
    public PostalAddressBuilder phone(@Nullable String value) {
        this.phone = value;
        return this;
    }

    /** The pickup/drop-off point id for point-based methods (optional). */
    public PostalAddressBuilder point(@Nullable String value) {
        this.point = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link PostalAddress}.
     *
     * @throws IllegalStateException if a required field is missing
     */
    public PostalAddress build() {
        String validStreet = BuilderValidation.requireText(street, FIELD_STREET);
        String validPostalCode = BuilderValidation.requireText(postalCode, FIELD_POSTAL_CODE);
        String validCity = BuilderValidation.requireText(city, FIELD_CITY);
        String validEmail = BuilderValidation.requireText(email, FIELD_EMAIL);
        String validPhone = BuilderValidation.requireText(phone, FIELD_PHONE);
        return new PostalAddress(name, company, validStreet, validPostalCode, validCity,
                state, validEmail, validPhone, point);
    }
}
