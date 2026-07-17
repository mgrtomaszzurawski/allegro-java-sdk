/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link WithdrawalAddress}. All fields except
 * {@code additionalInfo} are required; the string fields replicate Allegro's
 * maximum-length constraints so an over-long value fails here rather than on the
 * wire.
 *
 * @since 0.2.0
 */
public final class WithdrawalAddressBuilder {

    private static final int MAX_COMPANY_LENGTH = 200;
    private static final int MAX_STREET_LENGTH = 150;
    private static final int MAX_POSTAL_CODE_LENGTH = 12;
    private static final int MAX_CITY_LENGTH = 50;

    private static final String ERR_COMPANY = lengthConstraint("company", MAX_COMPANY_LENGTH);
    private static final String ERR_STREET = lengthConstraint("street", MAX_STREET_LENGTH);
    private static final String ERR_POSTAL_CODE = lengthConstraint("postalCode", MAX_POSTAL_CODE_LENGTH);
    private static final String ERR_CITY = lengthConstraint("city", MAX_CITY_LENGTH);
    private static final String ERR_COUNTRY_CODE = "countryCode is required";
    private static final String ERR_PHONE = "phone is required";

    private static String lengthConstraint(String field, int maxLength) {
        return field + " is required (1-" + maxLength + " chars)";
    }

    private @Nullable String company;
    private @Nullable String street;
    private @Nullable String postalCode;
    private @Nullable String city;
    private @Nullable String countryCode;
    private @Nullable PhoneNumber phone;
    private @Nullable String additionalInfo;

    /** Delivery recipient name (required, up to {@value #MAX_COMPANY_LENGTH} chars). */
    public WithdrawalAddressBuilder company(String company) {
        this.company = company;
        return this;
    }

    /** Street, building number, etc. (required, up to {@value #MAX_STREET_LENGTH} chars). */
    public WithdrawalAddressBuilder street(String street) {
        this.street = street;
        return this;
    }

    /** Postal code (required, up to {@value #MAX_POSTAL_CODE_LENGTH} chars). */
    public WithdrawalAddressBuilder postalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    /** City or town (required, up to {@value #MAX_CITY_LENGTH} chars). */
    public WithdrawalAddressBuilder city(String city) {
        this.city = city;
        return this;
    }

    /** Country code, e.g. {@code "PL"} (required). */
    public WithdrawalAddressBuilder countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    /** Contact phone number (required). */
    public WithdrawalAddressBuilder phone(PhoneNumber phone) {
        this.phone = phone;
        return this;
    }

    /** Optional extra delivery instructions. */
    public WithdrawalAddressBuilder additionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    /**
     * Validate and assemble the address.
     *
     * @throws IllegalStateException if a required field is missing or a string
     *     exceeds its maximum length
     */
    public WithdrawalAddress build() {
        return new WithdrawalAddress(
                bounded(company, MAX_COMPANY_LENGTH, ERR_COMPANY),
                bounded(street, MAX_STREET_LENGTH, ERR_STREET),
                bounded(postalCode, MAX_POSTAL_CODE_LENGTH, ERR_POSTAL_CODE),
                bounded(city, MAX_CITY_LENGTH, ERR_CITY),
                required(countryCode, ERR_COUNTRY_CODE),
                required(phone, ERR_PHONE),
                additionalInfo);
    }

    private static String bounded(@Nullable String value, int maxLength, String message) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static <T> T required(@Nullable T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
