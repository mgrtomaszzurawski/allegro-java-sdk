/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferPricingRules;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import java.util.List;

/**
 * Automatic pricing rules: the seller's reusable follow-the-market price
 * strategies (lowest price on Allegro, on the market, or the top offer, plus
 * exchange-rate conversion), each optionally adjusted by a fixed amount or a
 * percentage.
 *
 * <p>A rule is defined here once; assigning a rule to specific offers in bulk is
 * a batch-offer command owned by the offers facade, not this sub-facade. This
 * sub-facade only reads the rules currently assigned to one offer, via
 * {@link #rulesOfOffer(String)}.
 *
 * @since 0.2.0
 */
public interface PricingAutomation {

    /**
     * List all automatic pricing rules for the authenticated seller, including
     * the built-in default rules. The set is small and unpaginated, so it is
     * returned as a {@link List} rather than a stream.
     *
     * @return every rule the seller can assign
     */
    List<PricingRule> rules();

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
     * Edit an existing rule's name and configuration. A rule's type is fixed at
     * creation, so it cannot be changed here.
     *
     * @param ruleId the rule identifier
     * @param edit the new name and optional configuration, built with
     *     {@link PricingRuleEdit#builder()}
     * @return the updated rule
     */
    PricingRule update(String ruleId, PricingRuleEdit edit);

    /**
     * Delete an automatic pricing rule the seller created. Built-in default
     * rules cannot be deleted.
     *
     * @param ruleId the rule identifier
     */
    void delete(String ruleId);

    /**
     * Read the automatic pricing rules currently assigned to one offer, per
     * marketplace. Assignments are created in bulk through the offers facade;
     * this is a read-only view.
     *
     * @param offerId the offer identifier
     * @return the offer's rule assignments
     */
    OfferPricingRules rulesOfOffer(String offerId);
}
