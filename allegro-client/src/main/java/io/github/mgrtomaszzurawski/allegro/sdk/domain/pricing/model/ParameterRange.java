/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.util.Objects;

/**
 * A numeric range value for a category parameter (for example a weight or a
 * dimension expressed as a lower–upper span).
 *
 * @param lowerBound the inclusive lower bound
 * @param upperBound the inclusive upper bound
 *
 * @since 0.1.0
 */
public record ParameterRange(String lowerBound, String upperBound) {

    /** Compact constructor validating both bounds are present. */
    public ParameterRange {
        Objects.requireNonNull(lowerBound, "lowerBound");
        Objects.requireNonNull(upperBound, "upperBound");
    }
}
