/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.TurnoverDiscountRequestBuilder;
import java.util.List;

/**
 * The turnover-discount configuration to set for a marketplace: the ladder of
 * {@link TurnoverThreshold thresholds} that map cumulated turnover to a discount.
 * Build it with {@link #builder()}, which requires at least one threshold.
 *
 * @param thresholds the turnover-to-discount ladder (at least one)
 *
 * @since 0.3.0
 */
public record TurnoverDiscountRequest(List<TurnoverThreshold> thresholds) {

    /** Defensively copies the ladder so the request stays immutable. */
    public TurnoverDiscountRequest {
        thresholds = List.copyOf(thresholds);
    }

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link TurnoverDiscountRequestBuilder}
     */
    public static TurnoverDiscountRequestBuilder builder() {
        return new TurnoverDiscountRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's thresholds, for deriving a
     * modified copy.
     *
     * @return a builder holding this request's thresholds
     */
    public TurnoverDiscountRequestBuilder toBuilder() {
        return new TurnoverDiscountRequestBuilder().thresholds(thresholds);
    }
}
