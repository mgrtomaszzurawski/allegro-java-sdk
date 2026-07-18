/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRulesRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferPricingRules;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link PricingAutomation} facade. Maps the domain
 * request to the generated DTO, calls {@code /sale/price-automation/rules}, and
 * maps the response back to the public record.
 *
 * @since 0.2.0
 */
public final class PricingAutomationImpl implements PricingAutomation {

    private static final String OP_LIST_RULES = "list automatic pricing rules";
    private static final String OP_CREATE_RULE = "create automatic pricing rule";
    private static final String OP_GET_RULE = "get automatic pricing rule";
    private static final String OP_UPDATE_RULE = "update automatic pricing rule";
    private static final String OP_DELETE_RULE = "delete automatic pricing rule";
    private static final String OP_RULES_OF_OFFER = "get automatic pricing rules for offer";

    private final HttpSupport http;

    public PricingAutomationImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<PricingRule> rules() {
        JsonNode response = http.request(OP_LIST_RULES)
                .get(ApiPaths.PRICE_AUTOMATION_RULES)
                .fetch(JsonNode.class);
        return PricingMapper.toRules(response);
    }

    @Override
    public PricingRule create(PricingRuleRequest request) {
        JsonNode response = http.request(OP_CREATE_RULE)
                .post(ApiPaths.PRICE_AUTOMATION_RULES)
                .jsonBody(PricingMapper.toRaw(request))
                .fetch(JsonNode.class);
        return PricingMapper.toRule(response);
    }

    @Override
    public PricingRule get(String ruleId) {
        JsonNode response = http.request(OP_GET_RULE)
                .get(ApiPaths.subPath(ApiPaths.PRICE_AUTOMATION_RULES, ruleId))
                .fetch(JsonNode.class);
        return PricingMapper.toRule(response);
    }

    @Override
    public PricingRule update(String ruleId, PricingRuleEdit edit) {
        JsonNode response = http.request(OP_UPDATE_RULE)
                .put(ApiPaths.subPath(ApiPaths.PRICE_AUTOMATION_RULES, ruleId))
                .jsonBody(PricingMapper.editToRaw(edit))
                .fetch(JsonNode.class);
        return PricingMapper.toRule(response);
    }

    @Override
    public void delete(String ruleId) {
        http.request(OP_DELETE_RULE)
                .delete(ApiPaths.subPath(ApiPaths.PRICE_AUTOMATION_RULES, ruleId))
                .send();
    }

    @Override
    public OfferPricingRules rulesOfOffer(String offerId) {
        OfferRulesRaw response = http.request(OP_RULES_OF_OFFER)
                .get(ApiPaths.priceAutomationOfferRules(offerId))
                .fetch(OfferRulesRaw.class);
        return OfferPricingRules.from(response);
    }
}
