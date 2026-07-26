/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TaxExemptionRaw;

/**
 * A VAT exemption a seller may declare for a category, with a displayable
 * {@code label} and the wire {@code value}.
 *
 * @param label the human-readable exemption label
 * @param value the value to send back when configuring an offer
 *
 * @since 0.3.0
 */
public record TaxExemption(String label, String value) {

    /** Map the generated Layer-1 DTO. */
    public static TaxExemption from(TaxExemptionRaw raw) {
        return new TaxExemption(raw.getLabel(), raw.getValue());
    }
}
