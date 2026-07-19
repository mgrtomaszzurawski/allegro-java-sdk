/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRangeValueRaw;
import java.util.Objects;

/**
 * The value of a range (numeric or date) offer parameter — an inclusive
 * {@code lowerBound}&#8230;{@code upperBound} span carried as text, exactly as
 * Allegro types it on the wire (a category parameter dictates the unit and whether
 * it is numeric or a date).
 *
 * <p>Build one with {@link #of(String, String)} to set a range parameter on
 * {@code CreateOfferRequest}, or read one back from an {@link OfferParameter}. Both
 * bounds are required.
 *
 * @param lowerBound the lower bound (inclusive), the wire {@code from}
 * @param upperBound the upper bound (inclusive), the wire {@code to}
 * @since 0.3.0
 */
public record ParameterRange(String lowerBound, String upperBound) {

    /** Canonical constructor; both bounds are required. */
    public ParameterRange {
        Objects.requireNonNull(lowerBound, "lowerBound");
        Objects.requireNonNull(upperBound, "upperBound");
    }

    /** A range spanning {@code lowerBound}&#8230;{@code upperBound} (both inclusive, both required). */
    public static ParameterRange of(String lowerBound, String upperBound) {
        return new ParameterRange(lowerBound, upperBound);
    }

    /** The generated range value for this value. */
    public ParameterRangeValueRaw toRaw() {
        return new ParameterRangeValueRaw().from(lowerBound).to(upperBound);
    }
}
