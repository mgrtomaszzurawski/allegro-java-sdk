/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Benefit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferCriterion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionType;
import java.util.List;
import java.util.stream.Stream;

/**
 * Compile-only twin of the {@code docs/pricing.md} rebate-promotions snippets
 * (lazy listing, sealed-benefit matching, and the create/modify/deactivate
 * lifecycle). If the documented consumer code stops compiling, this module
 * breaks the build.
 */
public final class PricingPromotionsExample {

    private static final String CATEGORY_OFFER_ID = "12345";
    private static final String CURRENCY = "PLN";
    private static final int MAX_PROMOTIONS = 50;

    private PricingPromotionsExample() {
    }

    static Stream<Promotion> listLargeOrderPromotions(AllegroClient client) {
        return client.pricing().promotions()
                .streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT)
                .limit(MAX_PROMOTIONS);
    }

    static Stream<Promotion> listForOffer(AllegroClient client) {
        return client.pricing().promotions()
                .streamPromotions(PromotionType.WHOLESALE_PRICE_LIST, CATEGORY_OFFER_ID);
    }

    static String describeBenefits(AllegroClient client, String promotionId) {
        Promotion promotion = client.pricing().promotions().get(promotionId);
        StringBuilder summary = new StringBuilder();
        for (Benefit benefit : promotion.benefits()) {
            if (benefit instanceof Benefit.LargeOrderDiscount large) {
                summary.append("large-order tiers: ").append(large.thresholds().size());
            } else if (benefit instanceof Benefit.MultiPackDiscount multi) {
                summary.append("multipack ").append(multi.discountPercentage()).append('%');
            } else if (benefit instanceof Benefit.WholesalePriceList wholesale) {
                summary.append("wholesale ").append(wholesale.name());
            } else if (benefit instanceof Benefit.UnknownBenefit unknown) {
                summary.append("unknown ").append(unknown.type());
            }
        }
        return summary.toString();
    }

    static Promotion createLargeOrderPromotion(AllegroClient client) {
        return client.pricing().promotions().create(
                PromotionRequest.builder()
                        .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                                new Benefit.OrderValueThreshold(Money.of("100.00", CURRENCY), "10"))))
                        .addOfferCriterion(OfferCriterion.containing(List.of(CATEGORY_OFFER_ID)))
                        .build());
    }

    static Promotion modifyToAllOffers(AllegroClient client, String promotionId) {
        return client.pricing().promotions().modify(promotionId,
                PromotionRequest.builder()
                        .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                                new Benefit.OrderValueThreshold(Money.of("150.00", CURRENCY), "12"))))
                        .addOfferCriterion(OfferCriterion.allOffers())
                        .build());
    }

    static void deactivate(AllegroClient client, String promotionId) {
        client.pricing().promotions().deactivate(promotionId);
    }
}
