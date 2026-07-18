/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverThreshold;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link TurnoverDiscountRequest}. Add one or more
 * {@link TurnoverThreshold thresholds}; {@link #build()} fails fast if none were
 * supplied, since a turnover discount with no threshold sets nothing.
 *
 * @since 0.3.0
 */
public final class TurnoverDiscountRequestBuilder {

    private static final String ERR_NO_THRESHOLD = "at least one threshold is required";

    private final List<TurnoverThreshold> thresholds = new ArrayList<>();

    /**
     * Append one turnover threshold.
     *
     * @param threshold the turnover-to-discount threshold
     * @return this builder
     */
    public TurnoverDiscountRequestBuilder addThreshold(TurnoverThreshold threshold) {
        this.thresholds.add(threshold);
        return this;
    }

    /**
     * Replace the thresholds with the given list.
     *
     * @param newThresholds the turnover-to-discount ladder
     * @return this builder
     */
    public TurnoverDiscountRequestBuilder thresholds(List<TurnoverThreshold> newThresholds) {
        this.thresholds.clear();
        this.thresholds.addAll(newThresholds);
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if no threshold was supplied
     */
    public TurnoverDiscountRequest build() {
        if (thresholds.isEmpty()) {
            throw new IllegalStateException(ERR_NO_THRESHOLD);
        }
        return new TurnoverDiscountRequest(thresholds);
    }
}
