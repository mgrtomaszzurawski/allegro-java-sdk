/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyPeriodRaw;
import org.jspecify.annotations.Nullable;

/**
 * Duration of an implied warranty (rękojmia) for one buyer segment.
 *
 * <p>Unlike a seller {@link WarrantyPeriod}, an implied-warranty period has no
 * lifetime flag and the server accepts <strong>only whole years, at least
 * two</strong> (e.g. {@code P2Y}). Month-form values ({@code P12M}, {@code P24M})
 * and sub-two-year values ({@code P1Y}) are rejected with {@code 422} on the
 * {@code *.period} path (live-verified 2026-07-18, sandbox).
 *
 * @param period whole-year ISO-8601 duration of at least two years (e.g. {@code P2Y})
 *
 * @since 0.3.0
 */
public record ImpliedWarrantyPeriod(String period) {

    /** An implied-warranty period of the given whole-year ISO-8601 duration (min {@code P2Y}). */
    public static ImpliedWarrantyPeriod of(String isoPeriod) {
        return new ImpliedWarrantyPeriod(isoPeriod);
    }

    /** Map the generated Layer-1 DTO, or {@code null} when the field is absent. */
    public static @Nullable ImpliedWarrantyPeriod from(@Nullable ImpliedWarrantyPeriodRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ImpliedWarrantyPeriod(raw.getPeriod());
    }
}
