/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DimensionValueRaw;
import org.jspecify.annotations.Nullable;

/**
 * A single linear dimension carried on a delivery-option limit — the value and
 * its unit exactly as Allegro reports them (e.g. {@code "40"} / {@code "CENTIMETER"}).
 * Read-only: it appears only in a delivery proposal's limits.
 *
 * @param value the dimension amount as the exact string Allegro uses
 * @param unit the dimension unit (e.g. {@code "CENTIMETER"}), or {@code null}
 *
 * @since 0.5.0
 */
public record Measure(String value, @Nullable String unit) {

    /** Map the generated dimension DTO, or {@code null} when absent. */
    public static @Nullable Measure from(@Nullable DimensionValueRaw raw) {
        if (raw == null || raw.getValue() == null) {
            return null;
        }
        return new Measure(
                raw.getValue().toPlainString(),
                raw.getUnit() == null ? null : raw.getUnit().getValue());
    }
}
