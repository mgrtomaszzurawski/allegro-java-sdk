/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TaxSubjectRaw;

/**
 * A tax subject a seller may assign to an offer in a category (e.g. goods vs a
 * service), with a displayable {@code label} and the wire {@code value}.
 *
 * @param label the human-readable label
 * @param value the value to send back when configuring an offer
 *
 * @since 0.3.0
 */
public record TaxSubject(String label, String value) {

    /** Map the generated Layer-1 DTO. */
    public static TaxSubject from(TaxSubjectRaw raw) {
        return new TaxSubject(raw.getLabel(), raw.getValue());
    }
}
