/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * An automatic pricing rule owned by the authenticated seller.
 *
 * @param id server-assigned rule identifier
 * @param type the follow-the-market strategy the rule applies
 * @param name the rule name (built-in default rules are named by Allegro)
 * @param isDefault {@code true} for a built-in default rule, {@code false} for a
 *     rule the merchant created
 * @param updatedAt when the rule was last modified
 * @param configuration the amount/percentage adjustment, or {@code null} when
 *     the rule follows the market price without adjustment
 *
 * @since 0.2.0
 */
public record PricingRule(
        String id,
        PricingRuleType type,
        String name,
        boolean isDefault,
        Instant updatedAt,
        @Nullable PricingRuleConfiguration configuration) {
}
