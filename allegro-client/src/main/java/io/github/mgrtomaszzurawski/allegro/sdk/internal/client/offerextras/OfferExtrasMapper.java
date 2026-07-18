/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.BundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BundleMarketplaceDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemTextRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualDescriptionTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualProductSafetyInformationDescriptionTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualSafetyInformationTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualTitleTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ManualTranslationUpdateRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StandardizedDescriptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagIdsRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UpdateOfferBundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSectionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.ProductSafetyInformationTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.StandardizedDescription;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps the public offer-add-on request objects to their generated Layer-1
 * request bodies. Response mapping lives in the domain records' {@code from}
 * factories; only the write direction needs a mapper.
 */
final class OfferExtrasMapper {

    private static final String ERR_UNKNOWN_ITEM =
            "cannot write a description item of UNKNOWN type — build it with text(...) or image(...)";
    private static final String ERR_SAFETY_PRODUCT_ID_NULL =
            "a safety-information translation must have a productId — build it with of(productId, translation)";

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

    /**
     * Build the offer-translation update body from the parts the request set. Only
     * the set parts are attached; the impl serializes this as a partial (PATCH)
     * body so the unset parts are omitted rather than sent as {@code null}.
     */
    static ManualTranslationUpdateRequestRaw toRaw(TranslationRequest request) {
        ManualTranslationUpdateRequestRaw raw = new ManualTranslationUpdateRequestRaw();
        if (request.title() != null) {
            raw.title(new ManualTitleTranslationRaw().translation(request.title()));
        }
        if (request.description() != null) {
            raw.description(new ManualDescriptionTranslationRaw().translation(descriptionRaw(request.description())));
        }
        if (request.safetyInformation() != null) {
            raw.safetyInformation(safetyRaw(request.safetyInformation()));
        }
        return raw;
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

    private static StandardizedDescriptionRaw descriptionRaw(StandardizedDescription description) {
        StandardizedDescriptionRaw raw = new StandardizedDescriptionRaw();
        for (DescriptionSection section : description.sections()) {
            raw.addSectionsItem(sectionRaw(section));
        }
        return raw;
    }

    private static DescriptionSectionRaw sectionRaw(DescriptionSection section) {
        DescriptionSectionRaw raw = new DescriptionSectionRaw();
        for (DescriptionSectionItem item : section.items()) {
            raw.addItemsItem(itemRaw(item));
        }
        return raw;
    }

    private static DescriptionSectionItemRaw itemRaw(DescriptionSectionItem item) {
        return switch (item.type()) {
            case TEXT -> new DescriptionSectionItemTextRaw().content(item.content());
            case IMAGE -> new DescriptionSectionItemImageRaw().url(item.url());
            case UNKNOWN -> throw new IllegalArgumentException(ERR_UNKNOWN_ITEM);
        };
    }

    private static ManualSafetyInformationTranslationRaw safetyRaw(List<ProductSafetyInformationTranslation> products) {
        ManualSafetyInformationTranslationRaw raw = new ManualSafetyInformationTranslationRaw();
        for (ProductSafetyInformationTranslation product : products) {
            String productId = Objects.requireNonNull(product.productId(), ERR_SAFETY_PRODUCT_ID_NULL);
            raw.addProductsItem(new ManualProductSafetyInformationDescriptionTranslationRaw()
                    .id(UUID.fromString(productId))
                    .translation(product.translation()));
        }
        return raw;
    }
}
