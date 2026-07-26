/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferCriteriumRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPriceChangeCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationFixedPriceHolderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationFixedPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationValueChangeDecreaseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationValueChangeHolderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationValueChangeIncreaseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PriceChangeRequest;
import java.util.List;

/**
 * Builds the generated offer-price-change command body from the SDK's
 * {@link PriceChangeRequest}. Kept in the Layer-2 {@code mapping/} package (like
 * {@code OfferModificationMapper}) so the discriminated {@code PriceModification}
 * DTOs — the fixed / increase / decrease subtypes the wire requires — never leak
 * onto the Layer-3 builder's public surface. The subtype carries the {@code type}
 * discriminator itself; only the marketplace, when set, is attached explicitly.
 */
public final class PriceChangeMapper {

    private PriceChangeMapper() {
    }

    /** The command body for {@code request} (the command id travels in the path, not the body). */
    public static OfferPriceChangeCommandRaw toRaw(PriceChangeRequest request) {
        return new OfferPriceChangeCommandRaw()
                .offerCriteria(criteria(request))
                .modification(modification(request));
    }

    private static PriceModificationRaw modification(PriceChangeRequest request) {
        PriceModificationRaw modification = switch (request.kind()) {
            case FIXED -> new PriceModificationFixedPriceRaw().price(fixedHolder(request.amount()));
            case INCREASE -> new PriceModificationValueChangeIncreaseRaw().value(changeHolder(request.amount()));
            case DECREASE -> new PriceModificationValueChangeDecreaseRaw().value(changeHolder(request.amount()));
        };
        if (request.marketplaceId() != null) {
            modification.marketplaceId(request.marketplaceId());
        }
        return modification;
    }

    private static PriceModificationFixedPriceHolderRaw fixedHolder(Money price) {
        return new PriceModificationFixedPriceHolderRaw().amount(price.amount()).currency(price.currency());
    }

    private static PriceModificationValueChangeHolderRaw changeHolder(Money amount) {
        return new PriceModificationValueChangeHolderRaw().amount(amount.amount()).currency(amount.currency());
    }

    private static List<OfferCriteriumRaw> criteria(PriceChangeRequest request) {
        return List.of(new OfferCriteriumRaw()
                .type(OfferCriteriumRaw.TypeEnum.CONTAINS_OFFERS)
                .offers(request.offerIds().stream().map(id -> new OfferIdRaw().id(id)).toList()));
    }
}
