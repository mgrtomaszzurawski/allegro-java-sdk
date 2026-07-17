/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PhoneNumberWithCountryCodeRaw;

/**
 * An international phone number split into its country calling code and local
 * number, as Allegro models contact numbers on fulfillment addresses.
 *
 * @param countryCode country calling code without a {@code +} or {@code 0}
 *     prefix (e.g. {@code "48"}), 1–3 digits
 * @param number local number without a prefix (e.g. {@code "123123123"}),
 *     up to 15 characters
 *
 * @since 0.2.0
 */
public record PhoneNumber(String countryCode, String number) {

    private static final int MAX_COUNTRY_CODE_LENGTH = 3;
    private static final int MAX_NUMBER_LENGTH = 15;
    private static final String ERR_COUNTRY_CODE =
            "countryCode must be 1-" + MAX_COUNTRY_CODE_LENGTH + " characters";
    private static final String ERR_NUMBER =
            "number must be 1-" + MAX_NUMBER_LENGTH + " characters";

    public PhoneNumber {
        requireBounded(countryCode, MAX_COUNTRY_CODE_LENGTH, ERR_COUNTRY_CODE);
        requireBounded(number, MAX_NUMBER_LENGTH, ERR_NUMBER);
    }

    /** Build from a country calling code and a local number. */
    public static PhoneNumber of(String countryCode, String number) {
        return new PhoneNumber(countryCode, number);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static PhoneNumber from(PhoneNumberWithCountryCodeRaw raw) {
        return new PhoneNumber(raw.getCountryCode(), raw.getNumber());
    }

    private static void requireBounded(String value, int maxLength, String message) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }
}
