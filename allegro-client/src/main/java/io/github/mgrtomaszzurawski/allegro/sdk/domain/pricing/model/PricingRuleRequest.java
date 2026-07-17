/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.PricingRuleRequestBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Definition of an automatic pricing rule to create. Build it with
 * {@link #builder()}, which validates the required fields fail-fast.
 *
 * @param name the rule name (required, at most 33 characters)
 * @param type the follow-the-market strategy (required)
 * @param configuration the amount/percentage adjustment, or {@code null} for a
 *     pure follow rule
 *
 * @since 0.2.0
 */
public record PricingRuleRequest(
        String name,
        PricingRuleType type,
        @Nullable PricingRuleConfiguration configuration) {

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link PricingRuleRequestBuilder}
     */
    public static PricingRuleRequestBuilder builder() {
        return new PricingRuleRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's fields, for deriving a
     * modified copy.
     *
     * @return a builder holding this request's values
     */
    public PricingRuleRequestBuilder toBuilder() {
        return new PricingRuleRequestBuilder()
                .name(name)
                .type(type)
                .configuration(configuration);
    }
}
