/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmittedOfferDtoPricesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmittedOfferDtoProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmittedOfferDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer the seller has submitted to an AlleDiscount campaign, returned lazily
 * by {@code alleDiscount().streamSubmittedOffers(...)}. The {@code participationId}
 * is the key used to withdraw the offer.
 *
 * @param participationId     the participation id (withdraw key)
 * @param offerId             the offer
 * @param campaignId          the campaign
 * @param proposedPrice       the seller's proposed price, or {@code null}
 * @param minimalPriceReduction the minimal required reduction, or {@code null}
 * @param maximumSellingPrice the maximum selling price, or {@code null}
 * @param status              the participation lifecycle state
 * @param errors              why the submission was declined; empty otherwise
 * @param purchaseLimit       per-buyer purchase limit, or {@code null}
 *
 * @since 0.2.0
 */
public record AlleDiscountSubmittedOffer(
        String participationId,
        String offerId,
        String campaignId,
        @Nullable Money proposedPrice,
        @Nullable Money minimalPriceReduction,
        @Nullable Money maximumSellingPrice,
        AlleDiscountOfferStatus status,
        List<ConditionViolation> errors,
        @Nullable Integer purchaseLimit) {

    public AlleDiscountSubmittedOffer {
        errors = List.copyOf(errors);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static AlleDiscountSubmittedOffer from(AlleDiscountSubmittedOfferDtoRaw raw) {
        AlleDiscountSubmittedOfferDtoPricesRaw prices = raw.getPrices();
        AlleDiscountSubmittedOfferDtoProcessRaw process = raw.getProcess();
        return new AlleDiscountSubmittedOffer(
                raw.getParticipationId(),
                raw.getOffer().getId(),
                raw.getCampaign().getId(),
                proposedPrice(prices),
                minimalPriceReduction(prices),
                maximumSellingPrice(prices),
                AlleDiscountOfferStatus.from(process.getStatus()),
                errors(process),
                raw.getPurchaseLimit());
    }

    private static @Nullable Money proposedPrice(@Nullable AlleDiscountSubmittedOfferDtoPricesRaw prices) {
        return prices == null || prices.getProposedPrice() == null
                ? null
                : CampaignMappers.nullableMoney(
                        prices.getProposedPrice().getAmount(), prices.getProposedPrice().getCurrency());
    }

    private static @Nullable Money minimalPriceReduction(
            @Nullable AlleDiscountSubmittedOfferDtoPricesRaw prices) {
        return prices == null || prices.getMinimalPriceReduction() == null
                ? null
                : CampaignMappers.nullableMoney(prices.getMinimalPriceReduction().getAmount(),
                        prices.getMinimalPriceReduction().getCurrency());
    }

    private static @Nullable Money maximumSellingPrice(
            @Nullable AlleDiscountSubmittedOfferDtoPricesRaw prices) {
        return prices == null || prices.getMaximumSellingPrice() == null
                ? null
                : CampaignMappers.nullableMoney(prices.getMaximumSellingPrice().getAmount(),
                        prices.getMaximumSellingPrice().getCurrency());
    }

    private static List<ConditionViolation> errors(AlleDiscountSubmittedOfferDtoProcessRaw process) {
        return process.getErrors() == null
                ? List.of()
                : process.getErrors().stream().map(ConditionViolation::from).toList();
    }
}
