/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryTaxSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaxExemptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaxRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaxSubjectRaw;
import java.util.List;

/**
 * The tax options available for a category: the {@code subjects} and VAT
 * {@code rates} a seller may assign to an offer in that category, plus the
 * declarable {@code exemptions}. Read-only reference data returned by
 * {@code AllegroClient.settings().taxSettings(categoryId)}.
 *
 * @param subjects the assignable tax subjects
 * @param rates the VAT rates, grouped by country
 * @param exemptions the declarable VAT exemptions
 *
 * @since 0.3.0
 */
public record TaxSettings(
        List<TaxSubject> subjects,
        List<TaxRate> rates,
        List<TaxExemption> exemptions) {

    /** Canonical constructor — defensively copies the collections. */
    public TaxSettings {
        subjects = subjects == null ? List.of() : List.copyOf(subjects);
        rates = rates == null ? List.of() : List.copyOf(rates);
        exemptions = exemptions == null ? List.of() : List.copyOf(exemptions);
    }

    /** Map the generated Layer-1 DTO. */
    public static TaxSettings from(CategoryTaxSettingsRaw raw) {
        List<TaxSubjectRaw> rawSubjects = raw.getSubjects() == null ? List.of() : raw.getSubjects();
        List<TaxRateRaw> rawRates = raw.getRates() == null ? List.of() : raw.getRates();
        List<TaxExemptionRaw> rawExemptions = raw.getExemptions() == null ? List.of() : raw.getExemptions();
        return new TaxSettings(
                rawSubjects.stream().map(TaxSubject::from).toList(),
                rawRates.stream().map(TaxRate::from).toList(),
                rawExemptions.stream().map(TaxExemption::from).toList());
    }
}
