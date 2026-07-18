/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RestrictionCauseRaw;

/**
 * The reason a return policy restricts or disables returns (required when the
 * {@link ReturnRange} is {@code RESTRICTED} or {@code DISABLED}).
 *
 * <p>Allegro documents {@code ALCOHOL}, {@code FULLY_IMPLEMENTED_SERVICE} and
 * {@code BOOKED_SERVICE} as deprecated. The wire value and the constant name
 * coincide, so mapping fails loudly via {@link #valueOf(String)} on an
 * unmapped value — though in practice a future server value fails one layer
 * earlier, at Layer-1 deserialization, because the generated enum rejects it.
 * Graceful degradation of an unrecognised enum to a sentinel is a cross-cutting
 * concern owned by the (frozen) runtime ObjectMapper, tracked as a core
 * follow-up (shared with bucket E's category-parameter {@code type}).
 *
 * @since 0.3.0
 */
public enum ReturnRestrictionCause {

    /** Sealed audio/video/software returnable only if unopened. */
    SEALED_MEDIA,
    /** Sealed item non-returnable once unsealed for health/hygiene reasons. */
    SEALED_ITEM_NO_RETURN_DUE_HEALTH_OR_HYGIENE,
    /** Made to the buyer's specification / clearly personalised. */
    CUSTOM_ITEM,
    /** Perishable / short shelf life. */
    SHORT_SHELF_LIFE,
    /** Inseparably combined with another item after delivery. */
    INSEPARABLY_LINKED,
    /** Newspapers, periodicals or magazines. */
    PRESS,
    /** Medicinal product. */
    MEDICINAL_PRODUCT,
    /** Digital content not supplied on a tangible medium. */
    NOT_RECORDED_DIGITAL_CONTENT,
    /** Price dependent on financial-market fluctuations. */
    VALUE_DEPENDENT_ON_FINANCIAL_MARKET,
    /** Deprecated. */
    ALCOHOL,
    /** Deprecated. */
    FULLY_IMPLEMENTED_SERVICE,
    /** Deprecated. */
    BOOKED_SERVICE;

    /** Map the generated Layer-1 enum (names coincide with the wire values). */
    public static ReturnRestrictionCause from(RestrictionCauseRaw.NameEnum raw) {
        return valueOf(raw.name());
    }
}
