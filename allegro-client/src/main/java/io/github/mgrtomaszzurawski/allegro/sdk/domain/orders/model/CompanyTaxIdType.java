/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormInvoiceAddressCompanyIdRaw.TypeEnum;

/**
 * The kind of tax identifier a company holds on an order's invoice address. Each
 * constant name mirrors the Allegro spec value exactly.
 *
 * @since 0.7.0
 */
public enum CompanyTaxIdType {

    /** Polish NIP. */
    PL_NIP,

    /** EU VAT identifier. */
    VAT_EU,

    /** Czech DIČ (VAT identifier). */
    CZ_DIC,

    /** Czech IČO (business identifier). */
    CZ_ICO,

    /** Slovak IČO (business identifier). */
    SK_ICO,

    /** Slovak IČ DPH (VAT identifier). */
    SK_IC_DPH,

    /** Hungarian adószám (tax number). */
    HU_ADOSZAM,

    /** A tax-id kind outside the enumerated national types. */
    OTHER,

    /** A tax-id kind this SDK release does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /** Map the generated Layer-1 enum to the public tax-id type. */
    public static CompanyTaxIdType from(TypeEnum raw) {
        return switch (raw) {
            case PL_NIP -> PL_NIP;
            case VAT_EU -> VAT_EU;
            case CZ_DIC -> CZ_DIC;
            case CZ_ICO -> CZ_ICO;
            case SK_ICO -> SK_ICO;
            case SK_IC_DPH -> SK_IC_DPH;
            case HU_ADOSZAM -> HU_ADOSZAM;
            case OTHER -> OTHER;
            default -> UNKNOWN;
        };
    }
}
