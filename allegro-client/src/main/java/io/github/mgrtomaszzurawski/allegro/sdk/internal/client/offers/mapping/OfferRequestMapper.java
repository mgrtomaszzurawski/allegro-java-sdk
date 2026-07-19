/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestImpliedWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestReturnPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesProductOfferRequestWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ExternalIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferRequestRaw;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import java.util.List;
import java.util.UUID;

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
        if (!request.imageUrls().isEmpty()) {
            body.images(request.imageUrls());
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
        if (request.externalId() != null) {
            body.external(new ExternalIdRaw().id(request.externalId()));
        }
        if (request.language() != null) {
            body.language(request.language());
        }
        if (request.sizeTableId() != null) {
            body.sizeTable(new SizeTableRaw().id(request.sizeTableId()));
        }
        return body;
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
        if (services.impliedWarrantyId() != null) {
            raw.impliedWarranty(new AfterSalesServicesProductOfferRequestImpliedWarrantyRaw()
                    .id(UUID.fromString(services.impliedWarrantyId())));
        }
        if (services.returnPolicyId() != null) {
            raw.returnPolicy(new AfterSalesServicesProductOfferRequestReturnPolicyRaw()
                    .id(UUID.fromString(services.returnPolicyId())));
        }
        if (services.warrantyId() != null) {
            raw.warranty(new AfterSalesServicesProductOfferRequestWarrantyRaw()
                    .id(UUID.fromString(services.warrantyId())));
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
