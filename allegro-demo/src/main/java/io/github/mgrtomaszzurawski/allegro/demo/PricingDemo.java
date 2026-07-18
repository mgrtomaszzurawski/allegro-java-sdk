/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import java.io.IOException;

/**
 * Bucket G sandbox probe — the automatic-pricing-rule write→read→teardown cycle
 * through the SDK (TESTING.md §2). Creates a merchant rule, reads it back and
 * asserts the round-trip, then deletes it so the scenario cleans up after
 * itself. Demo entities carry the {@code [G-demo]} prefix and never touch
 * another bucket's data.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=pricing -Pdemo.account=seller
 * </pre>
 *
 * <p>Non-interactive: it uses the stored seller refresh token (run the
 * {@code auth-bootstrap} scenario once first). Output is status-level only —
 * never bodies or tokens.
 */
public final class PricingDemo {

    private static final String DEMO_RULE_NAME = "[G-demo] follow-allegro -5%";
    private static final String DEMO_PERCENTAGE = "5";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;

    private PricingDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            System.exit(EXIT_NO_TOKEN);
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            PricingAutomation automation = client.pricing().automation();

            // 1. create a merchant rule through the SDK
            PricingRule created = automation.create(PricingRuleRequest.builder()
                    .name(DEMO_RULE_NAME)
                    .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE)
                    .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                            PricingRuleConfiguration.Operation.SUBTRACT, DEMO_PERCENTAGE))
                    .build());
            System.out.println("created rule: id=" + created.id() + ", default=" + created.isDefault());

            // 2. read it back and assert the round-trip
            PricingRule fetched = automation.get(created.id());
            boolean matchesCreated = fetched.name().equals(created.name())
                    && fetched.type() == created.type()
                    && fetched.configuration() instanceof PricingRuleConfiguration.ChangeByPercentage;
            System.out.println("read-back matches: " + matchesCreated);

            // 3. tear down
            automation.delete(created.id());
            System.out.println("deleted rule: id=" + created.id());

            // Rotation: persist the refresh token Allegro issued on this run.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
        }
    }
}
