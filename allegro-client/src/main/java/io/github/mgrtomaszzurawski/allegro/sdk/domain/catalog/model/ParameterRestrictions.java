/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DictionaryCategoryParameterAllOfRestrictionsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FloatCategoryParameterAllOfRestrictionsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.IntegerCategoryParameterAllOfRestrictionsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StringCategoryProductParameterAllOfRestrictionsRaw;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * The constraints Allegro places on a {@link CategoryParameter}'s value. Which
 * components are populated follows the {@link CategoryParameterType}: numeric
 * parameters use {@code min}/{@code max}/{@code range} (and {@code precision}
 * for floats), string parameters use the length limits, and dictionary
 * parameters use {@code multipleChoices}. Components that do not apply are
 * {@code null} or {@code false}.
 *
 * @param minValue lowest allowed numeric value (float/integer), or {@code null}
 * @param maxValue highest allowed numeric value (float/integer), or {@code null}
 * @param precision digits allowed after the decimal point (float), or {@code null}
 * @param range whether two values ({@code from}/{@code to}) must be supplied
 *     (float/integer)
 * @param multipleChoices whether more than one dictionary value may be selected
 * @param minLength minimum text length (string), or {@code null}
 * @param maxLength maximum text length (string), or {@code null}
 * @param allowedNumberOfValues how many text values may be supplied (string),
 *     or {@code null}
 *
 * @since 0.2.0
 */
public record ParameterRestrictions(
        @Nullable BigDecimal minValue,
        @Nullable BigDecimal maxValue,
        @Nullable Integer precision,
        boolean range,
        boolean multipleChoices,
        @Nullable Integer minLength,
        @Nullable Integer maxLength,
        @Nullable Integer allowedNumberOfValues) {

    static @Nullable ParameterRestrictions fromFloat(
            @Nullable FloatCategoryParameterAllOfRestrictionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ParameterRestrictions(raw.getMin(), raw.getMax(), raw.getPrecision(),
                Boolean.TRUE.equals(raw.getRange()), false, null, null, null);
    }

    static @Nullable ParameterRestrictions fromInteger(
            @Nullable IntegerCategoryParameterAllOfRestrictionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ParameterRestrictions(toDecimal(raw.getMin()), toDecimal(raw.getMax()), null,
                Boolean.TRUE.equals(raw.getRange()), false, null, null, null);
    }

    static @Nullable ParameterRestrictions fromDictionary(
            @Nullable DictionaryCategoryParameterAllOfRestrictionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ParameterRestrictions(null, null, null, false,
                Boolean.TRUE.equals(raw.getMultipleChoices()), null, null, null);
    }

    static @Nullable ParameterRestrictions fromString(
            @Nullable StringCategoryProductParameterAllOfRestrictionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ParameterRestrictions(null, null, null, false, false,
                raw.getMinLength(), raw.getMaxLength(), raw.getAllowedNumberOfValues());
    }

    private static @Nullable BigDecimal toDecimal(@Nullable Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
