/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesRequestValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesRequestValueSellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestImpliedWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestReturnPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.B2bRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListManualRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DiscountsProductOfferRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DiscountsProductOfferRequestWholesalePriceListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ExternalIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferAdditionalServicesRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferAttachmentInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferFundraisingCampaignRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestBaseAllOfContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestV1AllOfDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestV1AllOfProductSetRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOffersRequestStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PublicationSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.CompatibilityEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.NamedReference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Builds the generated {@code SaleProductOfferRequestV1Raw} request body from the SDK's
 * {@link CreateOfferRequest} / {@link EditOfferRequest}. Kept out of {@code OffersImpl} so the
 * wiring hub is not coupled to every generated request DTO — this mapper is the one place that
 * knows the create/edit wire shape, in line with the Layer-2 {@code mapping/} package split.
 *
 * <p>Both bodies are serialized with {@code jsonBodyPartial} by the caller, so an unset optional
 * field is simply left absent here (never sent as {@code null}/{@code []}, which the server would
 * treat as a reset).
 *
 * @since 0.4.0
 */
public final class OfferRequestMapper {

    private OfferRequestMapper() {
    }

    /** The full create body: the required fields plus every optional field the request carries. */
    public static SaleProductOfferRequestV1Raw createBody(CreateOfferRequest request) {
        SaleProductOfferRequestV1Raw body = new SaleProductOfferRequestV1Raw()
                .name(request.name())
                .category(new OfferCategoryRequestRaw().id(request.categoryId()))
                .sellingMode(sellingModeOf(request))
                .stock(stockOf(request));
        applyContentFields(body, request);
        applyOfferMetadata(body, request);
        applyReferencesAndSettings(body, request);
        return body;
    }

    /** Media, delivery, description, location, parameters and the product set. */
    private static void applyContentFields(SaleProductOfferRequestV1Raw body, CreateOfferRequest request) {
        if (!request.imageUrls().isEmpty()) {
            body.images(request.imageUrls());
        }
        if (!request.attachmentIds().isEmpty()) {
            body.attachments(request.attachmentIds().stream()
                    .map(attachmentId -> new ProductOfferAttachmentInnerRaw().id(attachmentId)).toList());
        }
        if (request.delivery() != null) {
            body.delivery(deliveryRawOf(request.delivery()));
        }
        if (request.afterSalesServices() != null) {
            body.afterSalesServices(afterSalesRawOf(request.afterSalesServices()));
        }
        if (request.description() != null) {
            body.description(request.description().toRaw());
        }
        if (request.location() != null) {
            body.location(request.location().toRaw());
        }
        if (!request.parameters().isEmpty()) {
            body.parameters(parametersRawOf(request.parameters()));
        }
        if (!request.productSet().isEmpty()) {
            body.productSet(productSetRawOf(request.productSet()));
        }
        if (!request.compatibilityList().isEmpty()) {
            body.compatibilityList(compatibilityListRawOf(request.compatibilityList()));
        }
    }

    /** The generated manual "fits to" request body: each SDK entry becomes one union item, in order. */
    private static CompatibilityListManualRequestRaw compatibilityListRawOf(List<CompatibilityEntry> entries) {
        return new CompatibilityListManualRequestRaw()
                .items(entries.stream().map(CompatibilityEntry::toRaw).toList());
    }

    /** Identity, language, size table, B2B, publication and VAT settings. */
    private static void applyOfferMetadata(SaleProductOfferRequestV1Raw body, CreateOfferRequest request) {
        if (request.externalId() != null) {
            body.external(new ExternalIdRaw().id(request.externalId()));
        }
        if (request.language() != null) {
            body.language(request.language());
        }
        if (request.sizeTableId() != null) {
            body.sizeTable(new SizeTableRaw().id(request.sizeTableId()));
        }
        if (request.businessOnly() != null) {
            body.b2b(new B2bRaw().buyableOnlyByBusiness(request.businessOnly()));
        }
        if (request.publication() != null) {
            body.publication(publicationRawOf(request.publication()));
        }
        if (request.taxSettings() != null) {
            body.taxSettings(request.taxSettings().toRaw());
        }
    }

    /** Seller-registered references (contact, services, fundraising, discounts) and buyer/payment settings. */
    private static void applyReferencesAndSettings(
            SaleProductOfferRequestV1Raw body, CreateOfferRequest request) {
        if (request.contact() != null) {
            body.contact(namedRaw(request.contact(), SaleProductOfferRequestBaseAllOfContactRaw::new,
                    SaleProductOfferRequestBaseAllOfContactRaw::setId,
                    SaleProductOfferRequestBaseAllOfContactRaw::setName));
        }
        if (request.additionalServices() != null) {
            body.additionalServices(namedRaw(request.additionalServices(),
                    ProductOfferAdditionalServicesRequestRaw::new,
                    ProductOfferAdditionalServicesRequestRaw::setId,
                    ProductOfferAdditionalServicesRequestRaw::setName));
        }
        if (request.fundraisingCampaign() != null) {
            body.fundraisingCampaign(namedRaw(request.fundraisingCampaign(),
                    ProductOfferFundraisingCampaignRequestRaw::new,
                    ProductOfferFundraisingCampaignRequestRaw::setId,
                    ProductOfferFundraisingCampaignRequestRaw::setName));
        }
        if (request.wholesalePriceList() != null) {
            body.discounts(new DiscountsProductOfferRequestRaw().wholesalePriceList(
                    namedRaw(request.wholesalePriceList(), DiscountsProductOfferRequestWholesalePriceListRaw::new,
                            DiscountsProductOfferRequestWholesalePriceListRaw::setId,
                            DiscountsProductOfferRequestWholesalePriceListRaw::setName)));
        }
        if (request.messageToSellerSettings() != null) {
            body.messageToSellerSettings(request.messageToSellerSettings().toRaw());
        }
        if (request.payments() != null) {
            body.payments(request.payments().toRaw());
        }
        if (!request.additionalMarketplacePrices().isEmpty()) {
            body.additionalMarketplaces(additionalMarketplacesRawOf(request.additionalMarketplacePrices()));
        }
    }

    /** The generated per-marketplace request values: each marketplace id maps to its Buy Now price. */
    private static Map<String, AdditionalMarketplacesRequestValueRaw> additionalMarketplacesRawOf(
            Map<String, Money> prices) {
        Map<String, AdditionalMarketplacesRequestValueRaw> raw = new LinkedHashMap<>();
        prices.forEach((marketplaceId, price) -> raw.put(marketplaceId,
                new AdditionalMarketplacesRequestValueRaw().sellingMode(
                        new AdditionalMarketplacesRequestValueSellingModeRaw()
                                .price(new PriceRaw().amount(price.amount()).currency(price.currency())))));
        return raw;
    }

    /**
     * Build a generated id-or-name reference DTO: sets the id when the reference carries one, else
     * the name. The exactly-one-of invariant is enforced by {@link NamedReference} at construction.
     */
    private static <R> R namedRaw(NamedReference reference, Supplier<R> factory,
            BiConsumer<R, String> setId, BiConsumer<R, String> setName) {
        R raw = factory.get();
        if (reference.id() != null) {
            setId.accept(raw, reference.id());
        } else {
            setName.accept(raw, reference.name());
        }
        return raw;
    }

    /**
     * As {@link #namedRaw} but for DTOs whose {@code id} is a {@code UUID} on the wire: the id form
     * is parsed to a {@link UUID} (its UUID shape is already validated fail-fast by the builder).
     */
    private static <R> R namedUuidRaw(NamedReference reference, Supplier<R> factory,
            BiConsumer<R, UUID> setId, BiConsumer<R, String> setName) {
        R raw = factory.get();
        if (reference.id() != null) {
            setId.accept(raw, UUID.fromString(reference.id()));
        } else {
            setName.accept(raw, reference.name());
        }
        return raw;
    }

    /** The generated publication block for the SDK settings (only set fields are written). */
    private static SaleProductOfferPublicationRequestRaw publicationRawOf(PublicationSettings settings) {
        SaleProductOfferPublicationRequestRaw raw = new SaleProductOfferPublicationRequestRaw();
        if (settings.status() != null) {
            raw.status(settings.status().toRaw());
        }
        if (settings.startingAt() != null) {
            raw.startingAt(settings.startingAt());
        }
        if (settings.republish() != null) {
            raw.republish(settings.republish());
        }
        if (settings.duration() != null) {
            raw.duration(settings.duration().toString());
        }
        return raw;
    }

    /** The partial edit body: only the fields the request changed. */
    public static SaleProductOfferRequestV1Raw editBody(EditOfferRequest request) {
        SaleProductOfferRequestV1Raw body = new SaleProductOfferRequestV1Raw();
        if (request.name() != null) {
            body.name(request.name());
        }
        if (request.buyNowPrice() != null) {
            body.sellingMode(new SellingModeRaw().price(priceOf(request.buyNowPrice())));
        }
        if (request.availableStock() != null) {
            body.stock(new SaleProductOffersRequestStockRaw().available(request.availableStock()));
        }
        if (request.imageUrls() != null) {
            body.images(request.imageUrls());
        }
        return body;
    }

    /** The generated Buy Now price DTO for a {@link Money} amount. */
    private static BuyNowPriceRaw priceOf(Money money) {
        return new BuyNowPriceRaw().amount(money.amount()).currency(money.currency());
    }

    /** The generated selling mode for a create request: format, Buy Now price, and any auction prices. */
    private static SellingModeRaw sellingModeOf(CreateOfferRequest request) {
        SellingModeFormatRaw format = request.sellingFormat() == null
                ? SellingModeFormatRaw.BUY_NOW
                : request.sellingFormat().toRaw();
        SellingModeRaw sellingMode = new SellingModeRaw().format(format);
        if (request.buyNowPrice() != null) {
            sellingMode.price(priceOf(request.buyNowPrice()));
        }
        if (request.startingPrice() != null) {
            sellingMode.startingPrice(new StartingPriceRaw()
                    .amount(request.startingPrice().amount()).currency(request.startingPrice().currency()));
        }
        if (request.minimalPrice() != null) {
            sellingMode.minimalPrice(new MinimalPriceRaw()
                    .amount(request.minimalPrice().amount()).currency(request.minimalPrice().currency()));
        }
        return sellingMode;
    }

    /** The generated stock for a create request: available quantity and optional unit. */
    private static SaleProductOffersRequestStockRaw stockOf(CreateOfferRequest request) {
        SaleProductOffersRequestStockRaw stock =
                new SaleProductOffersRequestStockRaw().available(request.availableStock());
        if (request.stockUnit() != null) {
            stock.unit(request.stockUnit().toRaw());
        }
        return stock;
    }

    /** The generated delivery block for the SDK delivery terms (only set fields are written). */
    private static SaleProductOfferRequestV1AllOfDeliveryRaw deliveryRawOf(OfferDelivery delivery) {
        SaleProductOfferRequestV1AllOfDeliveryRaw raw = new SaleProductOfferRequestV1AllOfDeliveryRaw();
        if (delivery.shippingRatesId() != null) {
            raw.shippingRates(new JustIdRaw().id(delivery.shippingRatesId()));
        }
        if (delivery.handlingTime() != null) {
            raw.handlingTime(delivery.handlingTime());
        }
        if (delivery.shipmentDate() != null) {
            raw.shipmentDate(delivery.shipmentDate());
        }
        if (delivery.additionalInfo() != null) {
            raw.additionalInfo(delivery.additionalInfo());
        }
        return raw;
    }

    /** The generated after-sales block for the SDK conditions; the ids are parsed as Allegro UUIDs. */
    private static AfterSalesServicesProductOfferRequestRaw afterSalesRawOf(AfterSalesServices services) {
        AfterSalesServicesProductOfferRequestRaw raw = new AfterSalesServicesProductOfferRequestRaw();
        if (services.impliedWarranty() != null) {
            raw.impliedWarranty(namedUuidRaw(services.impliedWarranty(),
                    AfterSalesServicesProductOfferRequestImpliedWarrantyRaw::new,
                    AfterSalesServicesProductOfferRequestImpliedWarrantyRaw::setId,
                    AfterSalesServicesProductOfferRequestImpliedWarrantyRaw::setName));
        }
        if (services.returnPolicy() != null) {
            raw.returnPolicy(namedUuidRaw(services.returnPolicy(),
                    AfterSalesServicesProductOfferRequestReturnPolicyRaw::new,
                    AfterSalesServicesProductOfferRequestReturnPolicyRaw::setId,
                    AfterSalesServicesProductOfferRequestReturnPolicyRaw::setName));
        }
        if (services.warranty() != null) {
            raw.warranty(namedUuidRaw(services.warranty(),
                    AfterSalesServicesProductOfferRequestWarrantyRaw::new,
                    AfterSalesServicesProductOfferRequestWarrantyRaw::setId,
                    AfterSalesServicesProductOfferRequestWarrantyRaw::setName));
        }
        return raw;
    }

    /** The generated request parameters for the SDK category parameters, in order. */
    private static List<ParameterProductOfferRequestRaw> parametersRawOf(List<OfferParameter> parameters) {
        return parameters.stream().map(OfferParameter::toRaw).toList();
    }

    /** The generated request product-set elements for the SDK product bindings, in order. */
    private static List<SaleProductOfferRequestV1AllOfProductSetRaw> productSetRawOf(
            List<ProductSetElement> productSet) {
        return productSet.stream().map(ProductSetElement::toRaw).toList();
    }
}
