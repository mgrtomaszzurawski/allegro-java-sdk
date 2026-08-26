/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerAddressRaw;
import org.jspecify.annotations.Nullable;

/**
 * The registered address of a product-safety {@link ResponsibleProducer} (GPSR).
 *
 * @param countryCode the ISO country code, or {@code null}
 * @param street the street, or {@code null}
 * @param postalCode the postal code, or {@code null}
 * @param city the city, or {@code null}
 *
 * @since 0.4.0
 */
public record ProducerAddress(
        @Nullable String countryCode,
        @Nullable String street,
        @Nullable String postalCode,
        @Nullable String city) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ProducerAddress from(@Nullable ResponsibleProducerAddressRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ProducerAddress(
                raw.getCountryCode(), raw.getStreet(), raw.getPostalCode(), raw.getCity());
    }
}
