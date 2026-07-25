/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxRateRaw;
import org.jspecify.annotations.Nullable;

/**
 * A single VAT rate an offer declares for a marketplace country.
 *
 * <p>The same immutable value is used both ways: build one to set an offer's tax rate on
 * {@code CreateOfferRequest}, or read one back from an {@link Offer}.
 *
 * @param rate        the VAT rate as a percentage string (e.g. {@code "23"}, {@code "5"}), or {@code null}
 * @param countryCode the marketplace country the rate applies to (e.g. {@code "PL"}), or {@code null}
 * @since 0.5.0
 */
public record TaxRate(@Nullable String rate, @Nullable String countryCode) {

    /** A rate for a country. */
    public static TaxRate of(String rate, String countryCode) {
        return new TaxRate(rate, countryCode);
    }

    /**
     * Project a generated tax-rate onto the consumer value.
     *
     * @param raw the generated rate (may be {@code null})
     * @return the mapped value, or {@code null} if {@code raw} is {@code null}
     */
    public static @Nullable TaxRate from(@Nullable OfferTaxRateRaw raw) {
        return raw == null ? null : new TaxRate(raw.getRate(), raw.getCountryCode());
    }

    /** The generated tax-rate for this value (only set fields are written). */
    public OfferTaxRateRaw toRaw() {
        OfferTaxRateRaw raw = new OfferTaxRateRaw();
        if (rate != null) {
            raw.rate(rate);
        }
        if (countryCode != null) {
            raw.countryCode(countryCode);
        }
        return raw;
    }
}
