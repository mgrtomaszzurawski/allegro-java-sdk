/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TaxRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaxRateValueRaw;
import java.util.List;

/**
 * The VAT rates available for a category in one country.
 *
 * @param countryCode the country the rates apply to (e.g. {@code "PL"})
 * @param values the selectable VAT rates
 *
 * @since 0.3.0
 */
public record TaxRate(String countryCode, List<TaxRateValue> values) {

    /** Canonical constructor — defensively copies the values. */
    public TaxRate {
        values = values == null ? List.of() : List.copyOf(values);
    }

    /** Map the generated Layer-1 DTO. */
    public static TaxRate from(TaxRateRaw raw) {
        List<TaxRateValueRaw> rawValues = raw.getValues() == null ? List.of() : raw.getValues();
        List<TaxRateValue> values = rawValues.stream().map(TaxRateValue::from).toList();
        return new TaxRate(raw.getCountryCode(), values);
    }
}
