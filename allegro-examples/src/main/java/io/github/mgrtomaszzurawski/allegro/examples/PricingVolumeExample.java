/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferPricingRules;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverThreshold;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/pricing.md} volume snippets (fee preview,
 * fee quotes, turnover discounts, deposit types, and the completed automation
 * operations). If the documented consumer code stops compiling, this module
 * breaks the build.
 */
public final class PricingVolumeExample {

    private static final String CATEGORY_ID = "257";
    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String CURRENCY = "PLN";

    private PricingVolumeExample() {
    }

    static FeePreview feePreview(AllegroClient client) {
        return client.pricing().feePreview(OfferFeePreviewRequest.builder()
                .categoryId(CATEGORY_ID)
                .price(Money.of("99.99", CURRENCY))
                .marketplaceId(MARKETPLACE_ID)
                .build());
    }

    static List<OfferQuote> quotes(AllegroClient client) {
        return client.pricing().quotes(List.of("12345", "67890"));
    }

    static TurnoverDiscount setTurnoverDiscount(AllegroClient client) {
        return client.pricing().turnoverDiscounts().set(MARKETPLACE_ID,
                TurnoverDiscountRequest.builder()
                        .addThreshold(new TurnoverThreshold(Money.of("1000.00", CURRENCY), "5"))
                        .build());
    }

    static List<DepositType> depositTypes(AllegroClient client) {
        return client.pricing().depositTypes();
    }

    static PricingRule editRule(AllegroClient client, String ruleId) {
        return client.pricing().automation().update(ruleId,
                PricingRuleEdit.builder()
                        .name("Follow Allegro minus 8%")
                        .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                                PricingRuleConfiguration.Operation.SUBTRACT, "8"))
                        .build());
    }

    static OfferPricingRules rulesOfOffer(AllegroClient client, String offerId) {
        return client.pricing().automation().rulesOfOffer(offerId);
    }
}
