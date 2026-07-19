/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link PricingRuleRequest}. The {@code name} and
 * {@code type} are required; {@code configuration} is optional. {@link #build()}
 * validates the required fields and the server's 33-character name limit
 * fail-fast.
 *
 * @since 0.2.0
 */
public final class PricingRuleRequestBuilder {

    /** Server-side limit on the rule name length (spec {@code maxLength}). */
    private static final int NAME_MAX_LENGTH = 33;

    private static final String ERR_NAME_REQUIRED = "name is required";
    private static final String ERR_NAME_TOO_LONG =
            "name must be at most " + NAME_MAX_LENGTH + " characters";
    private static final String ERR_TYPE_REQUIRED = "type is required";
    private static final String ERR_TYPE_UNKNOWN =
            "type UNKNOWN is a read-only forward-compatibility value and cannot be used "
                    + "to create a rule";

    private @Nullable String name;
    private @Nullable PricingRuleType type;
    private @Nullable PricingRuleConfiguration configuration;

    /**
     * Set the rule name (required, at most 33 characters).
     *
     * @param ruleName the rule name
     * @return this builder
     */
    public PricingRuleRequestBuilder name(String ruleName) {
        this.name = ruleName;
        return this;
    }

    /**
     * Set the follow-the-market strategy (required).
     *
     * @param ruleType the rule type
     * @return this builder
     */
    public PricingRuleRequestBuilder type(PricingRuleType ruleType) {
        this.type = ruleType;
        return this;
    }

    /**
     * Set the optional amount/percentage adjustment.
     *
     * @param ruleConfiguration the adjustment, or {@code null} for a pure follow
     *     rule
     * @return this builder
     */
    public PricingRuleRequestBuilder configuration(@Nullable PricingRuleConfiguration ruleConfiguration) {
        this.configuration = ruleConfiguration;
        return this;
    }

    /**
     * Validate and build the request.
     *
     * @return the immutable request
     * @throws IllegalStateException if the name is missing or too long, the type
     *     is missing, or the type is the read-only {@link PricingRuleType#UNKNOWN}
     */
    public PricingRuleRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        if (type == null) {
            throw new IllegalStateException(ERR_TYPE_REQUIRED);
        }
        if (type == PricingRuleType.UNKNOWN) {
            throw new IllegalStateException(ERR_TYPE_UNKNOWN);
        }
        return new PricingRuleRequest(name, type, configuration);
    }
}
