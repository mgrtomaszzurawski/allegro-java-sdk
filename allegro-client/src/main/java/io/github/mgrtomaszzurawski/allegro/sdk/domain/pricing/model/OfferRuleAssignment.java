/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingOfferRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRulesRulesInnerRaw;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * One automatic pricing rule assigned to an offer on a specific marketplace,
 * together with the price band it operates within.
 *
 * @param marketplaceId the marketplace this assignment applies to
 * @param ruleId the identifier of the assigned {@link PricingRule}
 * @param updatedAt when this assignment was last changed
 * @param priceRange the price band the rule is confined to, or {@code null} when
 *     the assignment sets none
 *
 * @since 0.3.0
 */
public record OfferRuleAssignment(
        String marketplaceId,
        String ruleId,
        Instant updatedAt,
        @Nullable OfferRulePriceRange priceRange) {

    /**
     * Map one generated offer-rule entry to the public record.
     *
     * @param raw the generated offer-rule entry DTO
     * @return the mapped record
     */
    public static OfferRuleAssignment from(OfferRulesRulesInnerRaw raw) {
        AutomaticPricingOfferRuleConfigurationRaw configuration = raw.getConfiguration();
        OfferRulePriceRange priceRange = configuration == null || configuration.getPriceRange() == null
                ? null
                : OfferRulePriceRange.from(configuration.getPriceRange());
        return new OfferRuleAssignment(
                raw.getMarketplace().getId(),
                raw.getRule().getId(),
                raw.getUpdatedAt().toInstant(),
                priceRange);
    }
}
