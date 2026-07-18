/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.BundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BundleMarketplaceDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualTitleTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualTranslationUpdateRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagIdsRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UpdateOfferBundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import java.util.List;

/**
 * Maps the public offer-add-on request objects to their generated Layer-1
 * request bodies. Response mapping lives in the domain records' {@code from}
 * factories; only the write direction needs a mapper.
 */
final class OfferExtrasMapper {

    private OfferExtrasMapper() {
    }

    /** Build the create/rename tag body. */
    static TagRequestRaw toRaw(TagRequest request) {
        return new TagRequestRaw().name(request.name()).hidden(request.hidden());
    }

    /** Build the assign-tags-to-offer body from the tag ids. */
    static TagIdsRequestRaw toIdsRaw(List<String> tagIds) {
        TagIdsRequestRaw raw = new TagIdsRequestRaw();
        for (String tagId : tagIds) {
            raw.addTagsItem(new TagIdRaw().id(tagId));
        }
        return raw;
    }

    /** Build the offer-translation update body (title only, for now). */
    static ManualTranslationUpdateRequestRaw toRaw(TranslationRequest request) {
        return new ManualTranslationUpdateRequestRaw()
                .title(new ManualTitleTranslationRaw().translation(request.title()));
    }

    /** Build the bundle discount-update body from the per-marketplace discounts. */
    static UpdateOfferBundleDiscountDTORaw toDiscountsRaw(List<BundleDiscount> discounts) {
        List<BundleDiscountDTORaw> raw = discounts.stream()
                .map(discount -> new BundleDiscountDTORaw()
                        .marketplace(new BundleMarketplaceDTORaw().id(discount.marketplaceId()))
                        .amount(discount.amount().amount())
                        .currency(discount.amount().currency()))
                .toList();
        return new UpdateOfferBundleDiscountDTORaw().discounts(raw);
    }
}
