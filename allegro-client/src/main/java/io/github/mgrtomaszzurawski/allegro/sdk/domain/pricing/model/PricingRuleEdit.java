/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.PricingRuleEditBuilder;
import org.jspecify.annotations.Nullable;

/**
 * The editable fields of an existing automatic pricing rule. A rule's
 * {@link PricingRuleType type} is fixed at creation and cannot be changed, so an
 * edit carries only the {@code name} and the optional {@code configuration}.
 * Build it with {@link #builder()}, which validates the required name fail-fast.
 *
 * @param name the new rule name (required, at most 33 characters)
 * @param configuration the amount/percentage adjustment, or {@code null} to clear
 *     it back to a pure follow rule
 *
 * @since 0.3.0
 */
public record PricingRuleEdit(
        String name,
        @Nullable PricingRuleConfiguration configuration) {

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link PricingRuleEditBuilder}
     */
    public static PricingRuleEditBuilder builder() {
        return new PricingRuleEditBuilder();
    }

    /**
     * A builder pre-populated with this edit's fields, for deriving a modified
     * copy.
     *
     * @return a builder holding this edit's values
     */
    public PricingRuleEditBuilder toBuilder() {
        return new PricingRuleEditBuilder()
                .name(name)
                .configuration(configuration);
    }
}
