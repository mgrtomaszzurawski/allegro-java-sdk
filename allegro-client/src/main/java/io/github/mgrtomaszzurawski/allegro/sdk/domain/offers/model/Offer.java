/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AiCoCreatedContentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AiCoCreatedImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferAttachmentInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A seller's offer as returned by {@code offers().get(offerId)}.
 *
 * <p>Immutable projection of the vendor payload onto the fields consumers work
 * with day to day. A full product-offer read populates every field, but the
 * vendored spec marks none of the nested objects {@code required}, so mapping
 * tolerates their absence rather than risking an NPE on a partial payload:
 * {@code buyNowPrice} is {@code null} for an auction (which carries a starting
 * price) or when no selling mode is present; {@code availableStock} is
 * {@code null} for offers without a tracked quantity; {@code categoryId} is
 * {@code null} only if the payload omits the category.
 *
 * @param id             offer identifier
 * @param name           offer title
 * @param categoryId     Allegro category the offer is listed in, or {@code null}
 * @param format         how the offer is sold
 * @param status         publication status
 * @param buyNowPrice    fixed Buy Now price, or {@code null} for an auction
 * @param startingPrice  auction starting price, or {@code null} when not an auction
 * @param minimalPrice   auction minimal (reserve) price, or {@code null}
 * @param availableStock available quantity, or {@code null} when not tracked
 * @param stockUnit      the unit the stock is counted in, or {@code null} when not tracked
 * @param delivery       delivery terms (shipping-rate table, handling time), or
 *                       {@code null} if the payload omits them
 * @param afterSalesServices after-sales conditions (implied warranty, return
 *                       policy, warranty), or {@code null} if omitted
 * @param description    the standardized description (sections of text/images), or
 *                       {@code null} if omitted
 * @param location       the ship-from location, or {@code null} if omitted
 * @param parameters     the offer's category parameters (empty when the payload omits them)
 * @param externalId     the seller's own external identifier (their system's SKU/id), or {@code null}
 * @param language       the listing language (BCP-47 code, e.g. {@code pl-PL}), or {@code null} if omitted
 * @param sizeTableId    the id of the attached size table, or {@code null} if omitted
 * @param productSet     the offer's product-set elements (product bindings), empty when the
 *                       offer is not productized or the payload omits them
 * @param publication    publication lifecycle details (republish, start/end, base marketplace),
 *                       or {@code null} if the payload omits them
 * @param messageToSellerSettings the buyer-note settings (mode/hint), or {@code null}
 * @param payments       the payment settings (invoice type), or {@code null}
 * @param validation     Allegro's validation of the offer (blocking errors, non-blocking
 *                       warnings, validatedAt), or {@code null} if the payload omits it
 * @param businessOnly   {@code true} if the offer is buyable only by business buyers, or
 *                       {@code null} if the payload omits it
 * @param taxSettings    the offer's VAT settings (per-country rates, subject, exemption), or
 *                       {@code null} if the payload omits them
 * @param contactId      the id of the seller's contact attached to the offer, or {@code null}
 * @param additionalServicesGroupId the id of the seller's additional-services group, or {@code null}
 * @param fundraisingCampaignId the id of the fundraising campaign attached, or {@code null}
 * @param wholesalePriceListId the id of the seller's wholesale price list attached, or {@code null}
 * @param operationId    the id of the asynchronous create/edit operation that produced this
 *                       offer — pass it with {@link #id()} to {@code offers().operationStatus(...)}
 *                       to poll processing; {@code null} on a plain read (create/edit only)
 * @param additionalMarketplaces the offer's per-marketplace listing (pricing + publication state),
 *                       keyed by marketplace id; empty when the offer is not cross-listed
 * @param attachmentIds  the ids of the attachments linked to the offer; empty when none. Resolve
 *                       an id to its file name/url/type via {@code offers().media().getAttachment(id)}
 * @param aiCoCreatedImageUrls the URLs of the offer images declared as AI co-created; empty when none
 * @since 0.2.0
 */
public record Offer(
        String id,
        String name,
        @Nullable String categoryId,
        OfferFormat format,
        OfferStatus status,
        @Nullable Money buyNowPrice,
        @Nullable Money startingPrice,
        @Nullable Money minimalPrice,
        @Nullable Integer availableStock,
        @Nullable StockUnit stockUnit,
        @Nullable OfferDelivery delivery,
        @Nullable AfterSalesServices afterSalesServices,
        @Nullable OfferDescription description,
        @Nullable OfferLocation location,
        List<OfferParameter> parameters,
        @Nullable String externalId,
        @Nullable String language,
        @Nullable String sizeTableId,
        List<ProductSetElement> productSet,
        @Nullable OfferPublication publication,
        @Nullable MessageToSellerSettings messageToSellerSettings,
        @Nullable OfferPayments payments,
        @Nullable OfferValidation validation,
        @Nullable Boolean businessOnly,
        @Nullable TaxSettings taxSettings,
        @Nullable String contactId,
        @Nullable String additionalServicesGroupId,
        @Nullable String fundraisingCampaignId,
        @Nullable String wholesalePriceListId,
        @Nullable String operationId,
        Map<String, OfferMarketplace> additionalMarketplaces,
        List<String> attachmentIds,
        List<String> aiCoCreatedImageUrls) {

    /**
     * Canonical constructor. Normalizes the {@code parameters} and {@code productSet} lists and the
     * {@code additionalMarketplaces} map to immutable copies so the non-null "empty when the payload
     * omits them" contract holds on every construction path (the mapper already supplies immutable
     * collections).
     */
    public Offer {
        parameters = List.copyOf(parameters);
        productSet = List.copyOf(productSet);
        additionalMarketplaces = Map.copyOf(additionalMarketplaces);
        attachmentIds = List.copyOf(attachmentIds);
        aiCoCreatedImageUrls = List.copyOf(aiCoCreatedImageUrls);
    }

    /** Project a generated product-offer response onto the consumer record (a plain read). */
    public static Offer from(SaleProductOfferResponseV1Raw raw) {
        return from(raw, null);
    }

    /**
     * Project a generated product-offer response, carrying the id of the asynchronous create/edit
     * operation (from the response {@code Location} header) so the caller can poll processing.
     */
    public static Offer from(SaleProductOfferResponseV1Raw raw, @Nullable String operationId) {
        SellingModeRaw sellingMode = raw.getSellingMode();
        OfferCategoryRaw category = raw.getCategory();
        SaleProductOfferPublicationResponseRaw publication = raw.getPublication();
        return new Offer(
                raw.getId(),
                raw.getName(),
                category == null ? null : category.getId(),
                OfferFormat.from(sellingMode == null ? null : sellingMode.getFormat()),
                OfferStatus.from(publication == null ? null : publication.getStatus()),
                buyNowPriceOf(sellingMode),
                startingPriceOf(sellingMode),
                minimalPriceOf(sellingMode),
                availableStockOf(raw),
                stockUnitOf(raw),
                OfferDelivery.from(raw.getDelivery()),
                AfterSalesServices.from(raw.getAfterSalesServices()),
                OfferDescription.from(raw.getDescription()),
                OfferLocation.from(raw.getLocation()),
                parametersOf(raw),
                externalIdOf(raw),
                raw.getLanguage(),
                sizeTableIdOf(raw),
                productSetOf(raw),
                OfferPublication.from(publication),
                MessageToSellerSettings.from(raw.getMessageToSellerSettings()),
                OfferPayments.from(raw.getPayments()),
                OfferValidation.from(raw.getValidation()),
                businessOnlyOf(raw),
                TaxSettings.from(raw.getTaxSettings()),
                contactIdOf(raw),
                additionalServicesGroupIdOf(raw),
                fundraisingCampaignIdOf(raw),
                wholesalePriceListIdOf(raw),
                operationId,
                additionalMarketplacesOf(raw),
                attachmentIdsOf(raw),
                aiCoCreatedImageUrlsOf(raw));
    }

    private static List<String> attachmentIdsOf(SaleProductOfferResponseV1Raw raw) {
        List<ProductOfferAttachmentInnerRaw> attachments = raw.getAttachments();
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream()
                .map(ProductOfferAttachmentInnerRaw::getId).filter(Objects::nonNull).toList();
    }

    private static List<String> aiCoCreatedImageUrlsOf(SaleProductOfferResponseV1Raw raw) {
        AiCoCreatedContentRaw aiCoCreated = raw.getAiCoCreatedContent();
        if (aiCoCreated == null || aiCoCreated.getImages() == null) {
            return List.of();
        }
        return aiCoCreated.getImages().stream()
                .map(AiCoCreatedImageRaw::getUrl).filter(Objects::nonNull).toList();
    }

    private static Map<String, OfferMarketplace> additionalMarketplacesOf(SaleProductOfferResponseV1Raw raw) {
        Map<String, AdditionalMarketplacesResponseValueRaw> marketplaces = raw.getAdditionalMarketplaces();
        if (marketplaces == null || marketplaces.isEmpty()) {
            return Map.of();
        }
        Map<String, OfferMarketplace> mapped = new LinkedHashMap<>();
        marketplaces.forEach((marketplaceId, value) ->
                mapped.put(marketplaceId, OfferMarketplace.from(value)));
        return mapped;
    }

    private static @Nullable String contactIdOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getContact() == null ? null : raw.getContact().getId();
    }

    private static @Nullable String wholesalePriceListIdOf(SaleProductOfferResponseV1Raw raw) {
        var discounts = raw.getDiscounts();
        if (discounts == null || discounts.getWholesalePriceList() == null) {
            return null;
        }
        return discounts.getWholesalePriceList().getId();
    }

    private static @Nullable String additionalServicesGroupIdOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getAdditionalServices() == null ? null : raw.getAdditionalServices().getId();
    }

    private static @Nullable String fundraisingCampaignIdOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getFundraisingCampaign() == null ? null : raw.getFundraisingCampaign().getId();
    }

    private static @Nullable Boolean businessOnlyOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getB2b() == null ? null : raw.getB2b().getBuyableOnlyByBusiness();
    }

    private static List<OfferParameter> parametersOf(SaleProductOfferResponseV1Raw raw) {
        List<ParameterProductOfferResponseRaw> parameters = raw.getParameters();
        return parameters == null ? List.of() : parameters.stream().map(OfferParameter::from).toList();
    }

    private static List<ProductSetElement> productSetOf(SaleProductOfferResponseV1Raw raw) {
        List<SaleProductOfferResponseV1AllOfProductSetRaw> productSet = raw.getProductSet();
        return productSet == null ? List.of() : productSet.stream().map(ProductSetElement::from).toList();
    }

    private static @Nullable String externalIdOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getExternal() == null ? null : raw.getExternal().getId();
    }

    private static @Nullable String sizeTableIdOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getSizeTable() == null ? null : raw.getSizeTable().getId();
    }

    private static @Nullable Money buyNowPriceOf(@Nullable SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        BuyNowPriceRaw price = sellingMode.getPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money startingPriceOf(@Nullable SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        StartingPriceRaw price = sellingMode.getStartingPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money minimalPriceOf(@Nullable SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        MinimalPriceRaw price = sellingMode.getMinimalPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Integer availableStockOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getStock() == null ? null : raw.getStock().getAvailable();
    }

    private static @Nullable StockUnit stockUnitOf(SaleProductOfferResponseV1Raw raw) {
        StockRaw stock = raw.getStock();
        return stock == null || stock.getUnit() == null ? null : StockUnit.from(stock.getUnit());
    }
}
