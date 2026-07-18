/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyPeriodRaw;
import org.jspecify.annotations.Nullable;

/**
 * Duration of a warranty for one buyer segment (individual or corporate).
 *
 * <p>Either a fixed {@code period} (ISO-8601 duration, e.g. {@code P24M}) or a
 * {@code lifetime} warranty; a period value is ignored when {@code lifetime} is
 * {@code true}.
 *
 * @param period ISO-8601 duration (e.g. {@code P24M}), or {@code null} for a
 *     lifetime warranty
 * @param lifetime {@code true} when the warranty never expires
 *
 * @since 0.2.0
 */
public record WarrantyPeriod(@Nullable String period, boolean lifetime) {

    /** A fixed-length warranty for the given ISO-8601 duration. */
    public static WarrantyPeriod of(String isoPeriod) {
        return new WarrantyPeriod(isoPeriod, false);
    }

    /** A warranty that never expires. */
    public static WarrantyPeriod lifetimeWarranty() {
        return new WarrantyPeriod(null, true);
    }

    /** Map the generated Layer-1 DTO, or {@code null} when the field is absent. */
    public static @Nullable WarrantyPeriod from(@Nullable WarrantyPeriodRaw raw) {
        if (raw == null) {
            return null;
        }
        return new WarrantyPeriod(raw.getPeriod(), Boolean.TRUE.equals(raw.getLifetime()));
    }
}
