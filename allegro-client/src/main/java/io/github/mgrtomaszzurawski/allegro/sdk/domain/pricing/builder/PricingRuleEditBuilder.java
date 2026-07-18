/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link PricingRuleEdit}. The {@code name} is required;
 * {@code configuration} is optional. {@link #build()} validates the required
 * name and the server's 33-character limit fail-fast. The rule type is
 * immutable after creation and so is not part of an edit.
 *
 * @since 0.3.0
 */
public final class PricingRuleEditBuilder {

    /** Server-side limit on the rule name length (spec {@code maxLength}). */
    private static final int NAME_MAX_LENGTH = 33;

    private static final String ERR_NAME_REQUIRED = "name is required";
    private static final String ERR_NAME_TOO_LONG =
            "name must be at most " + NAME_MAX_LENGTH + " characters";

    private @Nullable String name;
    private @Nullable PricingRuleConfiguration configuration;

    /**
     * Set the new rule name (required, at most 33 characters).
     *
     * @param ruleName the rule name
     * @return this builder
     */
    public PricingRuleEditBuilder name(String ruleName) {
        this.name = ruleName;
        return this;
    }

    /**
     * Set the optional amount/percentage adjustment.
     *
     * @param ruleConfiguration the adjustment, or {@code null} to clear it
     * @return this builder
     */
    public PricingRuleEditBuilder configuration(@Nullable PricingRuleConfiguration ruleConfiguration) {
        this.configuration = ruleConfiguration;
        return this;
    }

    /**
     * Validate and build the edit.
     *
     * @return the immutable edit
     * @throws IllegalStateException if the name is missing or too long
     */
    public PricingRuleEdit build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        return new PricingRuleEdit(name, configuration);
    }
}
