/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesAddressRaw;
import java.util.Objects;

/**
 * A postal address attached to an after-sale condition — the address a buyer
 * uses to send an implied-warranty (rękojmia) or return claim.
 *
 * <p>All five fields are mandatory on the wire, so this record rejects a blank
 * value fail-fast at construction; a value obtained from the server therefore
 * always satisfies the same contract a consumer-built one does.
 *
 * @param name company or person name
 * @param street street name
 * @param postCode post code
 * @param city city name
 * @param countryCode country code (e.g. {@code PL})
 *
 * @since 0.3.0
 */
public record AfterSalesAddress(
        String name,
        String street,
        String postCode,
        String city,
        String countryCode) {

    private static final String ERR_NAME = "address name is required";
    private static final String ERR_STREET = "address street is required";
    private static final String ERR_POST_CODE = "address postCode is required";
    private static final String ERR_CITY = "address city is required";
    private static final String ERR_COUNTRY_CODE = "address countryCode is required";

    /** Canonical constructor — every field is required and must be non-blank. */
    public AfterSalesAddress {
        requireText(name, ERR_NAME);
        requireText(street, ERR_STREET);
        requireText(postCode, ERR_POST_CODE);
        requireText(city, ERR_CITY);
        requireText(countryCode, ERR_COUNTRY_CODE);
    }

    private static void requireText(String value, String message) {
        if (Objects.requireNonNull(value, message).isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static AfterSalesAddress from(AfterSalesServicesAddressRaw raw) {
        return new AfterSalesAddress(
                raw.getName(),
                raw.getStreet(),
                raw.getPostCode(),
                raw.getCity(),
                raw.getCountryCode());
    }
}
