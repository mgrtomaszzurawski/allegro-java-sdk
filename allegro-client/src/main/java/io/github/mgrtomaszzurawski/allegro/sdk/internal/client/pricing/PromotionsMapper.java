/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.client.model.BenefitRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BenefitSpecificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LargeOrderDiscountBenefitSpecificationAllOfDiscountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LargeOrderDiscountBenefitSpecificationAllOfOrderValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LargeOrderDiscountBenefitSpecificationAllOfThresholdsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LargeOrderDiscountBenefitSpecificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MultiPackBenefitSpecificationAllOfConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MultiPackBenefitSpecificationAllOfTriggerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MultiPackBenefitSpecificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerCreateRebateRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerRebateDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerRebateOfferCriterionOffersInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerRebateOfferCriterionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WholesalePriceListBenefitSpecificationAllOfQuantityRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WholesalePriceListBenefitSpecificationAllOfThresholdsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WholesalePriceListBenefitSpecificationRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Benefit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferCriterion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Package-private mapper between the promotions domain records and the Allegro
 * wire form.
 *
 * <p>The polymorphic {@code benefits[].specification} is deserialized natively
 * by Jackson (discriminated {@code @JsonTypeInfo}); an unknown discriminator
 * degrades to the base type via the shared forward-compat handler and is
 * surfaced here as {@link Benefit.UnknownBenefit} rather than failing the read.
 * Enum reads route unrecognised wire values (including the generated
 * forward-compat sentinel) to an {@code UNKNOWN} constant instead of throwing.
 */
final class PromotionsMapper {

    private static final String ERR_UNSERIALIZABLE_BENEFIT =
            "cannot serialize a benefit of type: ";
    private static final String ERR_UNSERIALIZABLE_CRITERION =
            "cannot serialize an unknown offer-criterion type";

    private PromotionsMapper() {
    }

    // ---- reads ----

    /** Map a promotion response DTO to the public record. */
    static Promotion from(SellerRebateDtoRaw raw) {
        var createdAt = raw.getCreatedAt();
        return new Promotion(
                raw.getId(),
                statusFrom(raw.getStatus()),
                createdAt == null ? null : createdAt.toInstant(),
                raw.getBenefits().stream().map(benefit -> benefitFrom(benefit.getSpecification())).toList(),
                raw.getOfferCriteria().stream().map(PromotionsMapper::criterionFrom).toList());
    }

    private static Promotion.Status statusFrom(SellerRebateDtoRaw.@Nullable StatusEnum raw) {
        // The generated enum deserializer yields null for a wire value this SDK
        // version does not model; degrade it (and the forward-compat sentinel)
        // to UNKNOWN rather than failing.
        if (raw == null) {
            return Promotion.Status.UNKNOWN;
        }
        return switch (raw) {
            case ACTIVE -> Promotion.Status.ACTIVE;
            case INACTIVE -> Promotion.Status.INACTIVE;
            case SUSPENDED -> Promotion.Status.SUSPENDED;
            default -> Promotion.Status.UNKNOWN;
        };
    }

    private static Benefit benefitFrom(BenefitSpecificationRaw raw) {
        if (raw instanceof LargeOrderDiscountBenefitSpecificationRaw large) {
            return new Benefit.LargeOrderDiscount(
                    large.getThresholds().stream().map(PromotionsMapper::orderValueThresholdFrom).toList());
        }
        if (raw instanceof MultiPackBenefitSpecificationRaw multi) {
            return new Benefit.MultiPackDiscount(
                    multi.getConfiguration().getPercentage().toPlainString(),
                    multi.getTrigger().getForEachQuantity(),
                    multi.getTrigger().getDiscountedNumber());
        }
        if (raw instanceof WholesalePriceListBenefitSpecificationRaw wholesale) {
            return new Benefit.WholesalePriceList(
                    wholesale.getName(),
                    wholesale.getThresholds().stream().map(PromotionsMapper::quantityThresholdFrom).toList());
        }
        // Forward-compat (C4): an unknown discriminator degraded to the base
        // type — surface it as a sentinel instead of failing the whole read.
        return new Benefit.UnknownBenefit(raw.getType());
    }

    private static Benefit.OrderValueThreshold orderValueThresholdFrom(
            LargeOrderDiscountBenefitSpecificationAllOfThresholdsRaw raw) {
        PriceRaw lowerBound = raw.getOrderValue().getLowerBound();
        return new Benefit.OrderValueThreshold(
                Money.of(lowerBound.getAmount(), lowerBound.getCurrency()),
                raw.getDiscount().getPercentage());
    }

    private static Benefit.QuantityThreshold quantityThresholdFrom(
            WholesalePriceListBenefitSpecificationAllOfThresholdsRaw raw) {
        return new Benefit.QuantityThreshold(
                raw.getQuantity().getLowerBound(),
                raw.getDiscount().getPercentage());
    }

    private static OfferCriterion criterionFrom(SellerRebateOfferCriterionRaw raw) {
        List<String> offerIds = raw.getOffers() == null
                ? List.of()
                : raw.getOffers().stream().map(SellerRebateOfferCriterionOffersInnerRaw::getId).toList();
        return new OfferCriterion(criterionTypeFrom(raw.getType()), offerIds);
    }

    private static OfferCriterion.Type criterionTypeFrom(SellerRebateOfferCriterionRaw.@Nullable TypeEnum raw) {
        // An unmodelled criterion type deserializes to null; degrade to UNKNOWN.
        if (raw == null) {
            return OfferCriterion.Type.UNKNOWN;
        }
        return switch (raw) {
            case CONTAINS_OFFERS -> OfferCriterion.Type.CONTAINS_OFFERS;
            case OFFERS_ASSIGNED_EXTERNALLY -> OfferCriterion.Type.OFFERS_ASSIGNED_EXTERNALLY;
            case ALL_OFFERS -> OfferCriterion.Type.ALL_OFFERS;
            default -> OfferCriterion.Type.UNKNOWN;
        };
    }

    // ---- writes ----

    /** Map a create/modify request to the generated request-body DTO. */
    static SellerCreateRebateRequestDtoRaw toRaw(PromotionRequest request) {
        return new SellerCreateRebateRequestDtoRaw()
                .benefits(request.benefits().stream().map(PromotionsMapper::benefitToRaw).toList())
                .offerCriteria(request.offerCriteria().stream().map(PromotionsMapper::criterionToRaw).toList());
    }

    private static BenefitRaw benefitToRaw(Benefit benefit) {
        return new BenefitRaw().specification(specificationToRaw(benefit));
    }

    private static BenefitSpecificationRaw specificationToRaw(Benefit benefit) {
        if (benefit instanceof Benefit.LargeOrderDiscount large) {
            return new LargeOrderDiscountBenefitSpecificationRaw()
                    .thresholds(large.thresholds().stream()
                            .map(PromotionsMapper::orderValueThresholdToRaw).toList());
        }
        if (benefit instanceof Benefit.MultiPackDiscount multi) {
            return new MultiPackBenefitSpecificationRaw()
                    ._configuration(new MultiPackBenefitSpecificationAllOfConfigurationRaw()
                            .percentage(new BigDecimal(multi.discountPercentage())))
                    .trigger(new MultiPackBenefitSpecificationAllOfTriggerRaw()
                            .forEachQuantity(multi.buyQuantity())
                            .discountedNumber(multi.discountedQuantity()));
        }
        if (benefit instanceof Benefit.WholesalePriceList wholesale) {
            return new WholesalePriceListBenefitSpecificationRaw()
                    .name(wholesale.name())
                    .thresholds(wholesale.thresholds().stream()
                            .map(PromotionsMapper::quantityThresholdToRaw).toList());
        }
        // UnknownBenefit (a read-only sentinel) cannot be written back.
        throw new IllegalArgumentException(
                ERR_UNSERIALIZABLE_BENEFIT + benefit.getClass().getSimpleName());
    }

    private static LargeOrderDiscountBenefitSpecificationAllOfThresholdsRaw orderValueThresholdToRaw(
            Benefit.OrderValueThreshold threshold) {
        Money orderValue = threshold.orderValueFrom();
        return new LargeOrderDiscountBenefitSpecificationAllOfThresholdsRaw()
                .orderValue(new LargeOrderDiscountBenefitSpecificationAllOfOrderValueRaw()
                        .lowerBound(new PriceRaw()
                                .amount(orderValue.amount())
                                .currency(orderValue.currency())))
                .discount(new LargeOrderDiscountBenefitSpecificationAllOfDiscountRaw()
                        .percentage(threshold.discountPercentage()));
    }

    private static WholesalePriceListBenefitSpecificationAllOfThresholdsRaw quantityThresholdToRaw(
            Benefit.QuantityThreshold threshold) {
        return new WholesalePriceListBenefitSpecificationAllOfThresholdsRaw()
                .quantity(new WholesalePriceListBenefitSpecificationAllOfQuantityRaw()
                        .lowerBound(threshold.quantityFrom()))
                .discount(new LargeOrderDiscountBenefitSpecificationAllOfDiscountRaw()
                        .percentage(threshold.discountPercentage()));
    }

    private static SellerRebateOfferCriterionRaw criterionToRaw(OfferCriterion criterion) {
        SellerRebateOfferCriterionRaw raw =
                new SellerRebateOfferCriterionRaw().type(criterionTypeToRaw(criterion.type()));
        if (!criterion.offerIds().isEmpty()) {
            raw.offers(criterion.offerIds().stream()
                    .map(offerId -> new SellerRebateOfferCriterionOffersInnerRaw().id(offerId))
                    .toList());
        }
        return raw;
    }

    private static SellerRebateOfferCriterionRaw.TypeEnum criterionTypeToRaw(OfferCriterion.Type type) {
        return switch (type) {
            case CONTAINS_OFFERS -> SellerRebateOfferCriterionRaw.TypeEnum.CONTAINS_OFFERS;
            case OFFERS_ASSIGNED_EXTERNALLY -> SellerRebateOfferCriterionRaw.TypeEnum.OFFERS_ASSIGNED_EXTERNALLY;
            case ALL_OFFERS -> SellerRebateOfferCriterionRaw.TypeEnum.ALL_OFFERS;
            case UNKNOWN -> throw new IllegalArgumentException(ERR_UNSERIALIZABLE_CRITERION);
        };
    }
}
