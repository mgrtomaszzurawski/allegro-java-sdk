/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TaxRateValueRaw;

/**
 * A single VAT rate a seller may pick for a category in one country: a
 * displayable {@code label}, its numeric {@code value} (a percentage as a string;
 * {@code "0"} for out-of-scope), and whether choosing it requires declaring an
 * exemption.
 *
 * @param label the human-readable rate label
 * @param value the numeric VAT rate as a string (e.g. {@code "23.00"})
 * @param exemptionRequired whether an exemption must be declared with this rate
 *
 * @since 0.3.0
 */
public record TaxRateValue(String label, String value, boolean exemptionRequired) {

    /** Map the generated Layer-1 DTO; a missing {@code exemptionRequired} reads false. */
    public static TaxRateValue from(TaxRateValueRaw raw) {
        boolean exemptionRequired = Boolean.TRUE.equals(raw.getExemptionRequired());
        return new TaxRateValue(raw.getLabel(), raw.getValue(), exemptionRequired);
    }
}
