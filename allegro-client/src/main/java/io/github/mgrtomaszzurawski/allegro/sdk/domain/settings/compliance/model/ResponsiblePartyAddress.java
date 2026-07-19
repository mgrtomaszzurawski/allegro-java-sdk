/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerAddressRaw;
import org.jspecify.annotations.Nullable;

/**
 * Postal address of a product-compliance responsible party (person or producer).
 *
 * <p>Shared by {@link ResponsiblePerson} and {@link ResponsibleProducer} — the two
 * wire schemas are structurally identical (ISO-3166 {@code countryCode}, street,
 * postal code, city). The person schema types {@code countryCode} as a closed
 * enum of EU country codes and the producer schema as a free two-letter string;
 * both map to a plain {@code String} here.
 *
 * @param countryCode ISO-3166 country code (e.g. {@code PL})
 * @param street street and building number
 * @param postalCode postal code
 * @param city city
 *
 * @since 0.3.0
 */
public record ResponsiblePartyAddress(
        @Nullable String countryCode,
        @Nullable String street,
        @Nullable String postalCode,
        @Nullable String city) {

    /** Map the generated person-address DTO, or {@code null} when absent. */
    public static @Nullable ResponsiblePartyAddress from(@Nullable ResponsiblePersonAddressRaw raw) {
        if (raw == null) {
            return null;
        }
        String country = raw.getCountryCode() == null ? null : raw.getCountryCode().getValue();
        return new ResponsiblePartyAddress(country, raw.getStreet(), raw.getPostalCode(), raw.getCity());
    }

    /** Map the generated producer-address DTO, or {@code null} when absent. */
    public static @Nullable ResponsiblePartyAddress from(@Nullable ResponsibleProducerAddressRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ResponsiblePartyAddress(
                raw.getCountryCode(), raw.getStreet(), raw.getPostalCode(), raw.getCity());
    }
}
