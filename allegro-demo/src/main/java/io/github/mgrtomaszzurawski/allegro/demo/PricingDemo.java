/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder.OfferFeePreviewRequestBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Promotions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import java.io.IOException;
import java.util.List;

/**
 * Bucket G sandbox probe — the automatic-pricing-rule write→read→teardown cycle
 * through the SDK (TESTING.md §2). Creates a merchant rule, reads it back and
 * asserts the round-trip, then deletes it so the scenario cleans up after
 * itself, read-shape-verifies rebate promotions (streaming each promotion
 * type), and previews offer fees by POSTing the full fee-affecting request body
 * and reading the computed fees back. Demo entities carry the {@code [G-demo]}
 * prefix and never touch another bucket's data.
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
    private static final int PROMOTIONS_SAMPLE_LIMIT = 5;
    private static final String FEE_CATEGORY_ID = "257";
    private static final String FEE_PRICE_AMOUNT = "99.99";
    private static final String FEE_CURRENCY = "PLN";
    private static final String FEE_MARKETPLACE = "allegro-pl";
    private static final String FEE_DURATION = "P30D";
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

            // 4. rebate promotions read-shape verify (read-only): stream each
            // required promotion type and log the mapped shape.
            Promotions promotions = client.pricing().promotions();
            for (PromotionType type : PromotionType.values()) {
                List<Promotion> sample = promotions.streamPromotions(type)
                        .limit(PROMOTIONS_SAMPLE_LIMIT).toList();
                System.out.println("promotions[" + type + "]: sampled " + sample.size());
                sample.stream().findFirst().ifPresent(promotion -> System.out.println(
                        "  id=" + promotion.id() + " status=" + promotion.status()
                                + " benefits=" + promotion.benefits().size()
                                + " criteria=" + promotion.offerCriteria().size()));
            }

            // 5. fee preview (read-only): POST the request body through the SDK
            // and read back the computed fees. Probe the bare category+price body
            // first, then the expanded fee-affecting body, so a server rejection
            // is pinned to the fields that caused it (the probe reports the field
            // errors instead of aborting the scenario).
            feePreviewProbe(client, "bare", OfferFeePreviewRequest.builder()
                    .categoryId(FEE_CATEGORY_ID)
                    .price(Money.of(FEE_PRICE_AMOUNT, FEE_CURRENCY)));
            feePreviewProbe(client, "expanded", OfferFeePreviewRequest.builder()
                    .categoryId(FEE_CATEGORY_ID)
                    .price(Money.of(FEE_PRICE_AMOUNT, FEE_CURRENCY))
                    .marketplaceId(FEE_MARKETPLACE)
                    .publicationDuration(FEE_DURATION)
                    .emphasizedForOneDay());

            // Rotation: persist the refresh token Allegro issued on this run.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
        }
    }

    private static void feePreviewProbe(
            AllegroClient client, String label, OfferFeePreviewRequestBuilder request) {
        try {
            FeePreview preview = client.pricing().feePreview(request.build());
            System.out.println("fee preview[" + label + "]: OK commissions="
                    + preview.commissions().size() + " quotes=" + preview.quotes().size());
        } catch (AllegroBadRequestException rejected) {
            rejected.errors().forEach(fieldError -> System.out.println(
                    "fee preview[" + label + "]: 400 path=" + fieldError.path()
                            + " code=" + fieldError.code()));
        }
    }
}
