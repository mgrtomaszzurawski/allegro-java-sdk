/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link PricingAutomation} facade. Maps the domain
 * request to the generated DTO, calls {@code /sale/price-automation/rules}, and
 * maps the response back to the public record.
 *
 * @since 0.2.0
 */
public final class PricingAutomationImpl implements PricingAutomation {

    private static final String OP_CREATE_RULE = "create automatic pricing rule";
    private static final String OP_GET_RULE = "get automatic pricing rule";
    private static final String OP_DELETE_RULE = "delete automatic pricing rule";

    private final HttpSupport http;

    public PricingAutomationImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
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
    public void delete(String ruleId) {
        http.request(OP_DELETE_RULE)
                .delete(ApiPaths.subPath(ApiPaths.PRICE_AUTOMATION_RULES, ruleId))
                .send();
    }
}
