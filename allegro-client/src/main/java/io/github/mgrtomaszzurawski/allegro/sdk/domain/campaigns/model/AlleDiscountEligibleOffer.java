/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountEligibleOfferDtoAlleDiscountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountEligibleOfferDtoBasePriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountEligibleOfferDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer eligible for an AlleDiscount campaign, returned lazily by
 * {@code alleDiscount().streamEligibleOffers(...)}: its base price, the
 * {@code requiredMerchantPrice} the seller must not exceed, the minimum
 * guaranteed discount, and whether the offer meets the campaign's conditions.
 *
 * @param offerId                          the offer
 * @param productId                        the product the offer is for, or {@code null}
 * @param basePrice                        the offer's base price, or {@code null}
 * @param requiredMerchantPrice            the price ceiling for participation, or {@code null}
 * @param minimumGuaranteedDiscountPercentage minimum guaranteed discount %, or {@code null}
 * @param meetsConditions                  whether the offer currently meets the campaign conditions
 * @param conditionViolations              why it does not; empty when {@code meetsConditions} is true
 *
 * @since 0.2.0
 */
public record AlleDiscountEligibleOffer(
        String offerId,
        @Nullable String productId,
        @Nullable Money basePrice,
        @Nullable Money requiredMerchantPrice,
        @Nullable String minimumGuaranteedDiscountPercentage,
        boolean meetsConditions,
        List<ConditionViolation> conditionViolations) {

    public AlleDiscountEligibleOffer {
        conditionViolations = List.copyOf(conditionViolations);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static AlleDiscountEligibleOffer from(AlleDiscountEligibleOfferDtoRaw raw) {
        AlleDiscountEligibleOfferDtoAlleDiscountRaw discount = raw.getAlleDiscount();
        return new AlleDiscountEligibleOffer(
                raw.getId(),
                raw.getProduct() == null ? null : raw.getProduct().getId(),
                basePrice(raw.getBasePrice()),
                requiredMerchantPrice(discount),
                minimumDiscount(discount),
                meetsConditions(discount),
                violations(discount));
    }

    private static @Nullable Money basePrice(@Nullable AlleDiscountEligibleOfferDtoBasePriceRaw price) {
        return price == null ? null : CampaignMappers.nullableMoney(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money requiredMerchantPrice(
            @Nullable AlleDiscountEligibleOfferDtoAlleDiscountRaw discount) {
        if (discount == null || discount.getRequiredMerchantPrice() == null) {
            return null;
        }
        return CampaignMappers.nullableMoney(
                discount.getRequiredMerchantPrice().getAmount(), discount.getRequiredMerchantPrice().getCurrency());
    }

    private static @Nullable String minimumDiscount(
            @Nullable AlleDiscountEligibleOfferDtoAlleDiscountRaw discount) {
        if (discount == null || discount.getMinimumGuaranteedDiscount() == null) {
            return null;
        }
        return discount.getMinimumGuaranteedDiscount().getPercentage();
    }

    private static boolean meetsConditions(
            @Nullable AlleDiscountEligibleOfferDtoAlleDiscountRaw discount) {
        return discount != null && discount.getCampaignConditions() != null
                && Boolean.TRUE.equals(discount.getCampaignConditions().getMeetsConditions());
    }

    private static List<ConditionViolation> violations(
            @Nullable AlleDiscountEligibleOfferDtoAlleDiscountRaw discount) {
        if (discount == null || discount.getCampaignConditions() == null
                || discount.getCampaignConditions().getViolations() == null) {
            return List.of();
        }
        return discount.getCampaignConditions().getViolations().stream()
                .map(ConditionViolation::from)
                .toList();
    }
}
