/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PublicationSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDescription;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferLocation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPayments;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.StockUnit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxSettings;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateOfferRequestTest {

    private static final String NAME = "Mechanical keyboard";
    private static final String CATEGORY_ID = "257";
    private static final Money PRICE = Money.of("199.99", "PLN");
    private static final int STOCK = 10;
    private static final String IMAGE_URL = "https://img.example/x.jpg";
    private static final String SHIPPING_RATES_ID = "a1b2c3d4-0000-0000-0000-000000000001";
    private static final String IMPLIED_WARRANTY_ID = "11111111-1111-1111-1111-111111111111";
    private static final Money STARTING_PRICE = Money.of("1.00", "PLN");
    private static final String PARAM_DICT_ID = "11321";
    private static final String PARAM_DICT_VALUE_ID = "1";
    private static final String PARAM_FREE_ID = "11324";
    private static final String PARAM_FREE_VALUE = "Cherry MX";
    private static final int EXPECTED_PARAM_COUNT = 2;
    private static final String PRODUCT_ID_A = "8f2b1c00-0000-4000-8000-00000000000a";
    private static final String PRODUCT_ID_B = "8f2b1c00-0000-4000-8000-00000000000b";
    private static final int PRODUCT_SET_QUANTITY = 2;
    private static final String EXTERNAL_ID = "SKU-12345";
    private static final String LANGUAGE = "pl-PL";
    private static final String SIZE_TABLE_ID = "size-table-1";
    private static final String CONTACT_ID = "contact-1";
    private static final String ADDITIONAL_SERVICES_ID = "8603fbbb-0f0e-4999-945e-258c4c96c7d6";
    private static final String FUNDRAISING_ID = "campaign-1";
    private static final String WHOLESALE_PRICE_LIST_ID = "wholesale-1";

    private static CreateOfferRequest.Builder validBuilder() {
        return CreateOfferRequest.builder()
                .name(NAME)
                .categoryId(CATEGORY_ID)
                .buyNowPrice(PRICE)
                .availableStock(STOCK);
    }

    @Test
    void build_whenAllRequiredFieldsSet_exposesEachValue() {
        // when
        CreateOfferRequest request = validBuilder().imageUrls(List.of(IMAGE_URL)).build();

        // then
        assertEquals(NAME, request.name());
        assertEquals(CATEGORY_ID, request.categoryId());
        assertEquals(PRICE, request.buyNowPrice());
        assertEquals(STOCK, request.availableStock());
        assertEquals(List.of(IMAGE_URL), request.imageUrls());
    }

    @Test
    void build_whenNoImages_defaultsToEmptyList() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertTrue(request.imageUrls().isEmpty());
    }

    @Test
    void build_whenDeliveryAndAfterSalesSet_exposesThem() {
        // given
        OfferDelivery delivery = OfferDelivery.builder().shippingRatesId(SHIPPING_RATES_ID).build();
        AfterSalesServices afterSales =
                AfterSalesServices.builder().impliedWarrantyId(IMPLIED_WARRANTY_ID).build();

        // when
        CreateOfferRequest request = validBuilder()
                .delivery(delivery).afterSalesServices(afterSales).build();

        // then
        assertEquals(delivery, request.delivery());
        assertEquals(afterSales, request.afterSalesServices());
    }

    @Test
    void build_whenFulfilmentNotSet_leavesThoseFieldsNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then — optional fulfilment blocks default to null (omitted from the wire)
        assertNull(request.delivery());
        assertNull(request.afterSalesServices());
    }

    @Test
    void build_whenSellingTermsSet_exposesFormatPricesAndUnit() {
        // given — an auction with starting and minimal prices, counted in pairs
        Money starting = Money.of("1.00", "PLN");
        Money minimal = Money.of("150.00", "PLN");

        // when
        CreateOfferRequest request = validBuilder()
                .sellingFormat(OfferFormat.AUCTION)
                .startingPrice(starting)
                .minimalPrice(minimal)
                .stockUnit(StockUnit.PAIR)
                .build();

        // then
        assertEquals(OfferFormat.AUCTION, request.sellingFormat());
        assertEquals(starting, request.startingPrice());
        assertEquals(minimal, request.minimalPrice());
        assertEquals(StockUnit.PAIR, request.stockUnit());
    }

    @Test
    void build_whenSellingTermsNotSet_leavesThemNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then — format/unit default at the mapper (BUY_NOW/UNIT), so the builder keeps null
        assertNull(request.sellingFormat());
        assertNull(request.startingPrice());
        assertNull(request.minimalPrice());
        assertNull(request.stockUnit());
    }

    @Test
    void build_whenNameMissing_throwsIllegalState() {
        // given — every required field but name
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .categoryId(CATEGORY_ID).buyNowPrice(PRICE).availableStock(STOCK);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenCategoryMissing_throwsIllegalState() {
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).buyNowPrice(PRICE).availableStock(STOCK);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenPriceMissing_throwsIllegalState() {
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).availableStock(STOCK);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenStockMissing_throwsIllegalState() {
        // given — availableStock never set
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(PRICE);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenStockNegative_throwsIllegalState() {
        CreateOfferRequest.Builder builder = validBuilder().availableStock(-1);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenPureAuction_succeedsWithoutBuyNowPrice() {
        // given — an auction with a starting price and NO Buy Now price
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).availableStock(STOCK)
                .sellingFormat(OfferFormat.AUCTION).startingPrice(STARTING_PRICE).build();

        // then — Buy Now is optional for an auction; the starting price stands in
        assertNull(request.buyNowPrice());
        assertEquals(STARTING_PRICE, request.startingPrice());
    }

    @Test
    void build_whenAuctionWithoutStartingPrice_throwsIllegalState() {
        // given — an auction missing its required starting price
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).availableStock(STOCK)
                .sellingFormat(OfferFormat.AUCTION);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenDescriptionAndLocationSet_exposesThem() {
        // given
        OfferDescription description = OfferDescription.of(
                DescriptionSection.of(DescriptionItem.text("<p>hello</p>")));
        OfferLocation location = OfferLocation.builder().city("Warszawa").countryCode("PL").build();

        // when
        CreateOfferRequest request = validBuilder()
                .description(description).location(location).build();

        // then
        assertEquals(description, request.description());
        assertEquals(location, request.location());
    }

    @Test
    void build_whenContentNotSet_leavesThoseFieldsNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.description());
        assertNull(request.location());
    }

    @Test
    void addParameter_whenCalledRepeatedly_accumulatesInOrder() {
        // when — two parameters added one at a time
        CreateOfferRequest request = validBuilder()
                .addParameter(OfferParameter.dictionary(PARAM_DICT_ID, PARAM_DICT_VALUE_ID))
                .addParameter(OfferParameter.freeText(PARAM_FREE_ID, PARAM_FREE_VALUE))
                .build();

        // then — both are exposed in the order added
        assertEquals(EXPECTED_PARAM_COUNT, request.parameters().size());
        assertEquals(PARAM_DICT_ID, request.parameters().get(0).id());
        assertEquals(PARAM_FREE_ID, request.parameters().get(1).id());
    }

    @Test
    void parameters_whenSetAsList_replacesTheParameters() {
        // given — a builder that already has one parameter added
        CreateOfferRequest.Builder builder = validBuilder()
                .addParameter(OfferParameter.dictionary(PARAM_DICT_ID, PARAM_DICT_VALUE_ID));

        // when — a bulk set replaces (does not append to) the accumulated parameters
        CreateOfferRequest request = builder
                .parameters(List.of(OfferParameter.freeText(PARAM_FREE_ID, PARAM_FREE_VALUE)))
                .build();

        // then — only the list's parameter remains
        assertEquals(1, request.parameters().size());
        assertEquals(PARAM_FREE_ID, request.parameters().get(0).id());
    }

    @Test
    void build_whenNoParameters_defaultsToEmptyList() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertTrue(request.parameters().isEmpty());
    }

    @Test
    void productSet_whenSetAsList_replacesTheAddedElements() {
        // given — a builder that already has one product-set element added
        CreateOfferRequest.Builder builder = validBuilder()
                .addProductSetElement(ProductSetElement.of(PRODUCT_ID_A));

        // when — a bulk set replaces (does not append to) the accumulated elements
        CreateOfferRequest request = builder
                .productSet(List.of(ProductSetElement.of(PRODUCT_ID_B, PRODUCT_SET_QUANTITY)))
                .build();

        // then — only the list's element remains
        assertEquals(1, request.productSet().size());
        assertEquals(PRODUCT_ID_B, request.productSet().get(0).productId());
        assertEquals(PRODUCT_SET_QUANTITY, request.productSet().get(0).quantity());
    }

    @Test
    void build_whenOfferRefsSet_exposesThem() {
        // when — the external id, listing language and size-table id are set
        CreateOfferRequest request = validBuilder()
                .externalId(EXTERNAL_ID).language(LANGUAGE).sizeTableId(SIZE_TABLE_ID).build();

        // then
        assertEquals(EXTERNAL_ID, request.externalId());
        assertEquals(LANGUAGE, request.language());
        assertEquals(SIZE_TABLE_ID, request.sizeTableId());
    }

    @Test
    void build_whenOfferRefsNotSet_leavesThemNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.externalId());
        assertNull(request.language());
        assertNull(request.sizeTableId());
    }

    @Test
    void build_whenBusinessOnlySet_exposesIt() {
        // when — the offer is restricted to business buyers
        CreateOfferRequest request = validBuilder().businessOnly(Boolean.TRUE).build();

        // then
        assertEquals(Boolean.TRUE, request.businessOnly());
    }

    @Test
    void build_whenBusinessOnlyNotSet_leavesItNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.businessOnly());
    }

    @Test
    void build_whenPublicationSet_exposesIt() {
        // given — publish immediately and auto-relist
        PublicationSettings publication = PublicationSettings.builder()
                .status(OfferStatus.ACTIVE).republish(Boolean.TRUE).build();

        // when
        CreateOfferRequest request = validBuilder().publication(publication).build();

        // then
        assertEquals(publication, request.publication());
    }

    @Test
    void build_whenPublicationNotSet_leavesItNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.publication());
    }

    @Test
    void build_whenTaxSettingsSet_exposesIt() {
        // given
        TaxSettings taxSettings = TaxSettings.builder()
                .subject("GOODS").rates(List.of(TaxRate.of("23", "PL"))).build();

        // when
        CreateOfferRequest request = validBuilder().taxSettings(taxSettings).build();

        // then
        assertEquals(taxSettings, request.taxSettings());
    }

    @Test
    void build_whenTaxSettingsNotSet_leavesItNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.taxSettings());
    }

    @Test
    void build_whenPaymentsSet_exposesIt() {
        // given
        OfferPayments payments = OfferPayments.of(InvoiceType.VAT);

        // when
        CreateOfferRequest request = validBuilder().payments(payments).build();

        // then
        assertEquals(payments, request.payments());
    }

    @Test
    void build_whenPaymentsNotSet_leavesItNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.payments());
    }

    @Test
    void build_whenReferenceIdsSet_exposeThem() {
        // when — the contact, additional-services group and fundraising campaign are attached by id
        CreateOfferRequest request = validBuilder()
                .contactId(CONTACT_ID)
                .additionalServicesGroupId(ADDITIONAL_SERVICES_ID)
                .fundraisingCampaignId(FUNDRAISING_ID)
                .wholesalePriceListId(WHOLESALE_PRICE_LIST_ID)
                .build();

        // then
        assertEquals(CONTACT_ID, request.contactId());
        assertEquals(ADDITIONAL_SERVICES_ID, request.additionalServicesGroupId());
        assertEquals(FUNDRAISING_ID, request.fundraisingCampaignId());
        assertEquals(WHOLESALE_PRICE_LIST_ID, request.wholesalePriceListId());
    }

    @Test
    void build_whenReferenceIdsNotSet_leaveThemNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.contactId());
        assertNull(request.additionalServicesGroupId());
        assertNull(request.fundraisingCampaignId());
        assertNull(request.wholesalePriceListId());
    }

    @Test
    void build_whenMessageToSellerSettingsSet_exposesThem() {
        // given
        MessageToSellerSettings settings =
                MessageToSellerSettings.of(MessageToSellerMode.REQUIRED, "note");

        // when
        CreateOfferRequest request = validBuilder().messageToSellerSettings(settings).build();

        // then
        assertEquals(settings, request.messageToSellerSettings());
    }

    @Test
    void build_whenMessageToSellerSettingsNotSet_leavesItNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertNull(request.messageToSellerSettings());
    }
}
