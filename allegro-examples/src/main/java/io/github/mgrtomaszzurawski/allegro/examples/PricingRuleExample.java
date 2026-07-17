/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;

/**
 * Compile-only twin of the {@code docs/pricing.md} automatic-pricing-rule
 * snippet — if the documented consumer code stops compiling, this module breaks
 * the build.
 */
public final class PricingRuleExample {

    private static final String RULE_NAME = "Follow Allegro minus 5%";
    private static final String PERCENTAGE = "5";

    private PricingRuleExample() {
    }

    static String createRule(String clientId, String clientSecret) {
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));

        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            PricingRule rule = client.pricing().automation().create(
                    PricingRuleRequest.builder()
                            .name(RULE_NAME)
                            .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE)
                            .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                                    PricingRuleConfiguration.Operation.SUBTRACT, PERCENTAGE))
                            .build());
            return rule.id();
        }
    }
}
