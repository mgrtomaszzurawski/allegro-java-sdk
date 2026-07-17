/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentWithdrawalAddressRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.WithdrawalAddressBuilder;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Where One Fulfillment returns a seller's goods when the removal operation is
 * {@link RemovalOperation#WITHDRAWAL}. Build one with
 * {@link io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.WithdrawalAddressBuilder},
 * which enforces the field-length constraints.
 *
 * @param company delivery recipient name
 * @param street street, building number, etc.
 * @param postalCode postal code
 * @param city city or town
 * @param countryCode country code (e.g. {@code "PL"})
 * @param phone contact phone number
 * @param additionalInfo optional extra delivery instructions, or {@code null}
 *
 * @since 0.2.0
 */
public record WithdrawalAddress(
        String company,
        String street,
        String postalCode,
        String city,
        String countryCode,
        PhoneNumber phone,
        @Nullable String additionalInfo) {

    private static final String ERR_COMPANY = "company must not be null";
    private static final String ERR_STREET = "street must not be null";
    private static final String ERR_POSTAL_CODE = "postalCode must not be null";
    private static final String ERR_CITY = "city must not be null";
    private static final String ERR_COUNTRY_CODE = "countryCode must not be null";
    private static final String ERR_PHONE = "phone must not be null";

    public WithdrawalAddress {
        Objects.requireNonNull(company, ERR_COMPANY);
        Objects.requireNonNull(street, ERR_STREET);
        Objects.requireNonNull(postalCode, ERR_POSTAL_CODE);
        Objects.requireNonNull(city, ERR_CITY);
        Objects.requireNonNull(countryCode, ERR_COUNTRY_CODE);
        Objects.requireNonNull(phone, ERR_PHONE);
    }

    /** A new builder for a withdrawal address. */
    public static WithdrawalAddressBuilder builder() {
        return new WithdrawalAddressBuilder();
    }

    /** A builder pre-populated with this address's values. */
    public WithdrawalAddressBuilder toBuilder() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .company(company)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .countryCode(countryCode)
                .phone(phone);
        if (additionalInfo != null) {
            builder.additionalInfo(additionalInfo);
        }
        return builder;
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static WithdrawalAddress from(FulfillmentWithdrawalAddressRaw raw) {
        return new WithdrawalAddress(
                raw.getCompany(),
                raw.getStreet(),
                raw.getPostalCode(),
                raw.getCity(),
                raw.getCountryCode(),
                PhoneNumber.from(raw.getPhone()),
                raw.getAdditionalInfo());
    }

    /**
     * Redacted on purpose: an address is personal data, and the SDK never lets
     * buyer/seller PII reach a log or exception message.
     */
    @Override
    public String toString() {
        return "WithdrawalAddress[redacted]";
    }
}
