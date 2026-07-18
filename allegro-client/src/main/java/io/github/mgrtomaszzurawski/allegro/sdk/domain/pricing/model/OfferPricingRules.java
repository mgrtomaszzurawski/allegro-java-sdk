/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferRulesRaw;
import java.time.Instant;
import java.util.List;

/**
 * The automatic pricing rules currently assigned to one offer, as returned by
 * {@code pricing().automation().rulesOfOffer(offerId)}. A read-only view: rules
 * are assigned to offers in bulk through the offers facade, not here.
 *
 * @param updatedAt when the offer's rule assignments were last changed
 * @param rules the per-marketplace rule assignments (possibly empty)
 *
 * @since 0.3.0
 */
public record OfferPricingRules(Instant updatedAt, List<OfferRuleAssignment> rules) {

    /** Defensively copies the assignments so the record stays immutable. */
    public OfferPricingRules {
        rules = List.copyOf(rules);
    }

    /**
     * Map the generated response DTO to the public record.
     *
     * @param raw the generated offer-rules DTO
     * @return the mapped record
     */
    public static OfferPricingRules from(OfferRulesRaw raw) {
        return new OfferPricingRules(
                raw.getUpdatedAt().toInstant(),
                raw.getRules().stream().map(OfferRuleAssignment::from).toList());
    }
}
