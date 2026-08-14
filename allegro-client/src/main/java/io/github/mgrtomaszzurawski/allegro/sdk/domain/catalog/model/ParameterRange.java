/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRangeValueRaw;
import org.jspecify.annotations.Nullable;

/**
 * The lower/upper bounds of a range-type {@link ProductParameterValue} (e.g. a
 * weight or size span) rather than discrete values, mapped from the wire
 * {@code from}/{@code to} fields.
 *
 * @param lower the lower bound (wire {@code from}), or {@code null}
 * @param upper the upper bound (wire {@code to}), or {@code null}
 *
 * @since 0.4.0
 */
public record ParameterRange(@Nullable String lower, @Nullable String upper) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ParameterRange from(@Nullable ParameterRangeValueRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ParameterRange(raw.getFrom(), raw.getTo());
    }
}
