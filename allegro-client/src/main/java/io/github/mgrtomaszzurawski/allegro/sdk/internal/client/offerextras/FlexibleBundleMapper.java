/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleCreateDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleMarketplaceDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleOfferDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleSlotDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleSlotDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleSlotsDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleUpdateDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleWholeBundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleOfferRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleSlotRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.MarketplaceDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.SlotDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.WholeBundleDiscount;
import java.util.List;
import java.util.Objects;

/**
 * Maps the public flexible-bundle create/update request objects to their generated
 * Layer-1 request bodies. Create and update share the same request shape, so both
 * mapping methods delegate to the same slot/discount helpers.
 */
final class FlexibleBundleMapper {

    private static final String ERR_UNKNOWN_DISCOUNT_TYPE =
            "cannot write a discount of UNKNOWN type — use FlexibleBundleDiscount.wholeBundle(...) or perSlot(...)";
    private static final String ERR_WHOLE_BUNDLE_NULL =
            "a WHOLE_BUNDLE_DISCOUNT must carry a wholeBundle — use FlexibleBundleDiscount.wholeBundle(...)";

    private FlexibleBundleMapper() {
    }

    /** Build the create-bundle body. */
    static FlexibleBundleCreateDTORaw toCreateRaw(FlexibleBundleRequest request) {
        FlexibleBundleCreateDTORaw raw = new FlexibleBundleCreateDTORaw();
        for (FlexibleBundleSlotRequest slot : request.slots()) {
            raw.addSlotsItem(slotRaw(slot));
        }
        if (request.discount() != null) {
            raw.discount(discountRaw(request.discount()));
        }
        return raw;
    }

    /** Build the update-bundle body (same shape as create). */
    static FlexibleBundleUpdateDTORaw toUpdateRaw(FlexibleBundleRequest request) {
        FlexibleBundleUpdateDTORaw raw = new FlexibleBundleUpdateDTORaw();
        for (FlexibleBundleSlotRequest slot : request.slots()) {
            raw.addSlotsItem(slotRaw(slot));
        }
        if (request.discount() != null) {
            raw.discount(discountRaw(request.discount()));
        }
        return raw;
    }

    private static FlexibleBundleSlotDTORaw slotRaw(FlexibleBundleSlotRequest slot) {
        FlexibleBundleSlotDTORaw raw = new FlexibleBundleSlotDTORaw()
                .order(slot.order())
                .entryPoint(slot.entryPoint())
                .requiredQuantity(slot.requiredQuantity());
        if (slot.id() != null) {
            raw.id(slot.id());
        }
        for (FlexibleBundleOfferRef offer : slot.offers()) {
            raw.addOffersItem(new FlexibleBundleOfferDTORaw()
                    .id(offer.offerId())
                    .excludedFromDiscount(offer.excludedFromDiscount()));
        }
        return raw;
    }

    private static FlexibleBundleDiscountDTORaw discountRaw(FlexibleBundleDiscount discount) {
        return switch (discount.type()) {
            case WHOLE_BUNDLE_DISCOUNT -> new FlexibleBundleDiscountDTORaw()
                    .type(FlexibleBundleDiscountDTORaw.TypeEnum.WHOLE_BUNDLE_DISCOUNT)
                    .bundle(wholeBundleRaw(Objects.requireNonNull(discount.wholeBundle(), ERR_WHOLE_BUNDLE_NULL)));
            case SLOT_DISCOUNT -> new FlexibleBundleDiscountDTORaw()
                    .type(FlexibleBundleDiscountDTORaw.TypeEnum.SLOT_DISCOUNT)
                    .slot(slotsDiscountRaw(discount.slotDiscounts()));
            case UNKNOWN -> throw new IllegalArgumentException(ERR_UNKNOWN_DISCOUNT_TYPE);
        };
    }

    private static FlexibleBundleWholeBundleDiscountDTORaw wholeBundleRaw(WholeBundleDiscount wholeBundle) {
        FlexibleBundleWholeBundleDiscountDTORaw raw = new FlexibleBundleWholeBundleDiscountDTORaw()
                .minimumBoughtOffers(wholeBundle.minimumBoughtOffers());
        for (MarketplaceDiscount marketplaceDiscount : wholeBundle.marketplaceDiscounts()) {
            raw.addDiscountsItem(marketplaceRaw(marketplaceDiscount));
        }
        return raw;
    }

    private static FlexibleBundleSlotsDiscountDTORaw slotsDiscountRaw(List<SlotDiscount> slotDiscounts) {
        FlexibleBundleSlotsDiscountDTORaw raw = new FlexibleBundleSlotsDiscountDTORaw();
        for (SlotDiscount slotDiscount : slotDiscounts) {
            FlexibleBundleSlotDiscountDTORaw slotRaw =
                    new FlexibleBundleSlotDiscountDTORaw().order(slotDiscount.order());
            for (MarketplaceDiscount marketplaceDiscount : slotDiscount.marketplaceDiscounts()) {
                slotRaw.addDiscountsItem(marketplaceRaw(marketplaceDiscount));
            }
            raw.addSlotsItem(slotRaw);
        }
        return raw;
    }

    private static FlexibleBundleMarketplaceDiscountDTORaw marketplaceRaw(MarketplaceDiscount marketplaceDiscount) {
        return new FlexibleBundleMarketplaceDiscountDTORaw()
                .marketplaceId(marketplaceDiscount.marketplaceId())
                .percentage(marketplaceDiscount.percentage());
    }
}
