/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundBankAccountAddressRaw;
import org.jspecify.annotations.Nullable;

/**
 * The address tied to a return refund's {@link ReturnBankAccount}. Personal data —
 * its {@link #toString()} is redacted so an accidental log never leaks it; read the
 * accessors deliberately.
 *
 * @param street the street, or {@code null}
 * @param city the city, or {@code null}
 * @param postCode the postal code, or {@code null}
 * @param countryCode the country code, or {@code null}
 *
 * @since 0.7.0
 */
public record ReturnBankAddress(
        @Nullable String street,
        @Nullable String city,
        @Nullable String postCode,
        @Nullable String countryCode) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnBankAddress from(CustomerReturnRefundBankAccountAddressRaw raw) {
        return new ReturnBankAddress(
                raw.getStreet(), raw.getCity(), raw.getPostCode(), raw.getCountryCode());
    }

    /** Redacts the address (personal data); read the accessors deliberately. */
    @Override
    public String toString() {
        return "ReturnBankAddress[redacted]";
    }
}
