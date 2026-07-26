/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsRaw;
import org.jspecify.annotations.Nullable;

/**
 * The kind of invoice a seller issues for an offer.
 *
 * @since 0.6.0
 */
public enum InvoiceType {

    /** A standard VAT invoice. */
    VAT,
    /** A VAT-margin invoice (second-hand goods, art, antiques). */
    VAT_MARGIN,
    /** An invoice without VAT. */
    WITHOUT_VAT,
    /** The seller issues no invoice. */
    NO_INVOICE,
    /** An invoice type this SDK release does not model yet. */
    UNKNOWN;

    private static final String ERR_NOT_SETTABLE =
            "invoice type is not a value a client can request: ";

    /** Map the generated invoice type, tolerating unknown future values; {@code null} maps to {@code null}. */
    public static @Nullable InvoiceType from(PaymentsRaw.@Nullable InvoiceEnum raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case VAT -> VAT;
            case VAT_MARGIN -> VAT_MARGIN;
            case WITHOUT_VAT -> WITHOUT_VAT;
            case NO_INVOICE -> NO_INVOICE;
            default -> UNKNOWN;
        };
    }

    /**
     * Map to the generated invoice type a client may request; {@link #UNKNOWN} is not a real
     * type and is rejected.
     *
     * @throws IllegalArgumentException if the invoice type cannot be requested by a client
     */
    public PaymentsRaw.InvoiceEnum toRaw() {
        return switch (this) {
            case VAT -> PaymentsRaw.InvoiceEnum.VAT;
            case VAT_MARGIN -> PaymentsRaw.InvoiceEnum.VAT_MARGIN;
            case WITHOUT_VAT -> PaymentsRaw.InvoiceEnum.WITHOUT_VAT;
            case NO_INVOICE -> PaymentsRaw.InvoiceEnum.NO_INVOICE;
            case UNKNOWN -> throw new IllegalArgumentException(ERR_NOT_SETTABLE + this);
        };
    }
}
