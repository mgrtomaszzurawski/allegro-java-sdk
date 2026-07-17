/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;

/**
 * Automatic pricing rules: the seller's reusable follow-the-market price
 * strategies (lowest price on Allegro, on the market, or the top offer, plus
 * exchange-rate conversion), each optionally adjusted by a fixed amount or a
 * percentage.
 *
 * <p>A rule is defined here once; assigning a rule to specific offers in bulk is
 * a batch-offer command owned by the offers facade, not this sub-facade. Reading
 * the rules currently assigned to one offer lands with the bucket's volume PR.
 *
 * @since 0.2.0
 */
public interface PricingAutomation {

    /**
     * Create a new automatic pricing rule for the authenticated seller.
     *
     * @param request the rule definition (name, type, optional configuration),
     *     built with {@link PricingRuleRequest#builder()}
     * @return the created rule, including its server-assigned id
     */
    PricingRule create(PricingRuleRequest request);

    /**
     * Fetch a single automatic pricing rule by its identifier.
     *
     * @param ruleId the rule identifier
     * @return the rule
     */
    PricingRule get(String ruleId);

    /**
     * Delete an automatic pricing rule the seller created. Built-in default
     * rules cannot be deleted.
     *
     * @param ruleId the rule identifier
     */
    void delete(String ruleId);
}
