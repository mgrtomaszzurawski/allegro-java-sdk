/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingOfferRuleConfigurationPriceRangeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingOfferRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingCommandModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingModificationRemoveRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingModificationRemoveRemoveInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingModificationSetRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingModificationSetSetInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCriteriumRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRulesRulesInnerMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRulesRulesInnerRuleRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.RuleAssignment;
import java.util.List;
import java.util.UUID;

/**
 * Builds the generated automatic-pricing command body from the SDK's
 * {@link BatchPricingRulesRequest}. Kept in the Layer-2 {@code mapping/} package
 * (like {@code BulkOfferModificationMapper}) so the wire shape — and the generated
 * {@code *Raw} DTOs, including the {@code oneOf} modification wrapper — never leak
 * onto the Layer-3 builder's public surface.
 *
 * <p>The request's offers become a single {@code CONTAINS_OFFERS} criterion; its
 * mode ({@link BatchPricingRulesRequest#isAssignment()}) selects the {@code set}
 * or {@code remove} branch of the modification.
 */
public final class PricingRulesMapper {

    private PricingRulesMapper() {
    }

    /** The command body for {@code request}, with {@code commandId} as its id. */
    public static OfferAutomaticPricingCommandRaw toRaw(UUID commandId, BatchPricingRulesRequest request) {
        OfferCriteriumRaw criterion = new OfferCriteriumRaw()
                .type(OfferCriteriumRaw.TypeEnum.CONTAINS_OFFERS)
                .offers(request.offerIds().stream().map(id -> new OfferIdRaw().id(id)).toList());
        return new OfferAutomaticPricingCommandRaw()
                .id(commandId)
                .modification(request.isAssignment() ? setModification(request) : removeModification(request))
                .offerCriteria(List.of(criterion));
    }

    private static OfferAutomaticPricingCommandModificationRaw setModification(
            BatchPricingRulesRequest request) {
        OfferAutomaticPricingModificationSetRaw setBranch = new OfferAutomaticPricingModificationSetRaw()
                .set(request.assignments().stream().map(PricingRulesMapper::setItem).toList());
        return new OfferAutomaticPricingCommandModificationRaw(setBranch);
    }

    private static OfferAutomaticPricingModificationSetSetInnerRaw setItem(RuleAssignment assignment) {
        OfferAutomaticPricingModificationSetSetInnerRaw item =
                new OfferAutomaticPricingModificationSetSetInnerRaw()
                        .marketplace(new OfferRulesRulesInnerMarketplaceRaw().id(assignment.marketplaceId()))
                        .rule(new OfferRulesRulesInnerRuleRaw().id(assignment.ruleId()));
        PriceRange configuration = assignment.configuration();
        if (configuration != null) {
            item.setConfiguration(new AutomaticPricingOfferRuleConfigurationRaw()
                    .priceRange(priceRangeRaw(configuration)));
        }
        return item;
    }

    private static AutomaticPricingOfferRuleConfigurationPriceRangeRaw priceRangeRaw(PriceRange range) {
        return new AutomaticPricingOfferRuleConfigurationPriceRangeRaw()
                .type(currencyType(range.currencyBasis()))
                .minPrice(price(range.minPrice()))
                .maxPrice(price(range.maxPrice()));
    }

    private static AutomaticPricingOfferRuleConfigurationPriceRangeRaw.TypeEnum currencyType(
            PriceRange.CurrencyBasis basis) {
        return basis == PriceRange.CurrencyBasis.BASE_MARKETPLACE_CURRENCY
                ? AutomaticPricingOfferRuleConfigurationPriceRangeRaw.TypeEnum.BASE_MARKETPLACE_CURRENCY
                : AutomaticPricingOfferRuleConfigurationPriceRangeRaw.TypeEnum.MARKETPLACE_CURRENCY;
    }

    private static OfferAutomaticPricingCommandModificationRaw removeModification(
            BatchPricingRulesRequest request) {
        OfferAutomaticPricingModificationRemoveRaw remove = new OfferAutomaticPricingModificationRemoveRaw()
                .remove(request.removalMarketplaceIds().stream()
                        .map(id -> new OfferAutomaticPricingModificationRemoveRemoveInnerRaw()
                                .marketplace(new OfferRulesRulesInnerMarketplaceRaw().id(id)))
                        .toList());
        return new OfferAutomaticPricingCommandModificationRaw(remove);
    }

    private static PriceRaw price(Money money) {
        return new PriceRaw().amount(money.amount()).currency(money.currency());
    }
}
