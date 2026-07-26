/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationDiscountsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationDiscountsWholesalePriceListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationPublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationResponsiblePersonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModificationResponsibleProducerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferChangeCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCriteriumRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRatesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.HandlingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferDuration;
import java.util.List;

/**
 * Builds the generated offer-modification command body from the SDK's
 * {@link BatchModificationRequest}. Kept in the Layer-2 {@code mapping/} package
 * (like {@code PricingRulesMapper}) so the broad generated {@code Modification}
 * DTO — with its ten optional sub-objects — never leaks onto the Layer-3
 * builder's public surface. Only the sub-objects the request actually sets are
 * attached; the rest stay absent (the body is written partial).
 */
public final class OfferModificationMapper {

    private OfferModificationMapper() {
    }

    /** The command body for {@code request} (the command id travels in the path, not the body). */
    public static OfferChangeCommandRaw toRaw(BatchModificationRequest request) {
        OfferCriteriumRaw criterion = new OfferCriteriumRaw()
                .type(OfferCriteriumRaw.TypeEnum.CONTAINS_OFFERS)
                .offers(request.offerIds().stream().map(id -> new OfferIdRaw().id(id)).toList());
        return new OfferChangeCommandRaw()
                .modification(modification(request))
                .offerCriteria(List.of(criterion));
    }

    private static ModificationRaw modification(BatchModificationRequest request) {
        ModificationRaw modification = new ModificationRaw();
        if (request.unlimitedListing()) {
            modification.publication(new ModificationPublicationRaw().durationUnlimited(true));
        } else if (request.listingDuration() != null) {
            modification.publication(new ModificationPublicationRaw()
                    .duration(durationEnum(request.listingDuration())));
        }
        if (request.handlingTime() != null) {
            modification.delivery(new ModificationDeliveryRaw()
                    .handlingTime(handlingTimeEnum(request.handlingTime())));
        }
        if (request.shippingRatesId() != null) {
            modification.delivery(new ModificationDeliveryRaw()
                    .shippingRates(new ShippingRatesRaw().id(request.shippingRatesId())));
        }
        if (request.wholesalePriceListId() != null) {
            modification.discounts(new ModificationDiscountsRaw().wholesalePriceList(
                    new ModificationDiscountsWholesalePriceListRaw().id(request.wholesalePriceListId())));
        }
        if (request.sizeTableId() != null) {
            modification.sizeTable(new SizeTableRaw().id(request.sizeTableId()));
        }
        if (request.additionalServicesGroupId() != null) {
            modification.additionalServicesGroup(
                    new AdditionalServicesGroupRaw().id(request.additionalServicesGroupId()));
        }
        if (request.responsibleProducerId() != null) {
            modification.responsibleProducer(
                    new ModificationResponsibleProducerRaw().id(request.responsibleProducerId()));
        }
        if (request.responsiblePersonId() != null) {
            modification.responsiblePerson(
                    new ModificationResponsiblePersonRaw().id(request.responsiblePersonId()));
        }
        return modification;
    }

    private static ModificationPublicationRaw.DurationEnum durationEnum(OfferDuration duration) {
        return switch (duration) {
            case DAYS_3 -> ModificationPublicationRaw.DurationEnum.P3_D;
            case DAYS_5 -> ModificationPublicationRaw.DurationEnum.P5_D;
            case DAYS_7 -> ModificationPublicationRaw.DurationEnum.P7_D;
            case DAYS_10 -> ModificationPublicationRaw.DurationEnum.P10_D;
            case DAYS_20 -> ModificationPublicationRaw.DurationEnum.P20_D;
            case DAYS_30 -> ModificationPublicationRaw.DurationEnum.P30_D;
        };
    }

    private static ModificationDeliveryRaw.HandlingTimeEnum handlingTimeEnum(HandlingTime handlingTime) {
        return switch (handlingTime) {
            case IMMEDIATE -> ModificationDeliveryRaw.HandlingTimeEnum.PT0_S;
            case DAY_1 -> ModificationDeliveryRaw.HandlingTimeEnum.PT24_H;
            case DAYS_2 -> ModificationDeliveryRaw.HandlingTimeEnum.P2_D;
            case DAYS_3 -> ModificationDeliveryRaw.HandlingTimeEnum.P3_D;
            case DAYS_4 -> ModificationDeliveryRaw.HandlingTimeEnum.P4_D;
            case DAYS_5 -> ModificationDeliveryRaw.HandlingTimeEnum.P5_D;
            case DAYS_7 -> ModificationDeliveryRaw.HandlingTimeEnum.P7_D;
            case DAYS_10 -> ModificationDeliveryRaw.HandlingTimeEnum.P10_D;
            case DAYS_14 -> ModificationDeliveryRaw.HandlingTimeEnum.P14_D;
            case DAYS_21 -> ModificationDeliveryRaw.HandlingTimeEnum.P21_D;
            case DAYS_30 -> ModificationDeliveryRaw.HandlingTimeEnum.P30_D;
            case DAYS_60 -> ModificationDeliveryRaw.HandlingTimeEnum.P60_D;
        };
    }
}
