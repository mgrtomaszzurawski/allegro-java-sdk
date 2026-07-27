/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A category parameter value carried on a fee-preview offer. A parameter holds
 * its values in exactly one shape: free-text {@code values}, dictionary
 * {@code valuesIds}, or a numeric {@code rangeValue}. Build one with the factory
 * that matches the parameter's kind.
 *
 * @param id the parameter id (required)
 * @param values free-text or numeric values, empty when not a value parameter
 * @param valuesIds dictionary value ids, empty when not a dictionary parameter
 * @param rangeValue the numeric range, or {@code null} when not a range parameter
 *
 * @since 0.1.0
 */
public record OfferParameter(
        String id,
        List<String> values,
        List<String> valuesIds,
        @Nullable ParameterRange rangeValue) {

    /** Compact constructor validating the id and taking defensive list copies. */
    public OfferParameter {
        Objects.requireNonNull(id, "id");
        values = List.copyOf(values);
        valuesIds = List.copyOf(valuesIds);
    }

    /**
     * A parameter carrying free-text or numeric values.
     *
     * @param id the parameter id
     * @param values the values
     * @return the parameter
     */
    public static OfferParameter ofValues(String id, List<String> values) {
        return new OfferParameter(id, values, List.of(), null);
    }

    /**
     * A parameter carrying dictionary value ids.
     *
     * @param id the parameter id
     * @param valuesIds the dictionary value ids
     * @return the parameter
     */
    public static OfferParameter ofValueIds(String id, List<String> valuesIds) {
        return new OfferParameter(id, List.of(), valuesIds, null);
    }

    /**
     * A parameter carrying a numeric range.
     *
     * @param id the parameter id
     * @param range the numeric range
     * @return the parameter
     */
    public static OfferParameter ofRange(String id, ParameterRange range) {
        return new OfferParameter(id, List.of(), List.of(), Objects.requireNonNull(range, "range"));
    }
}
