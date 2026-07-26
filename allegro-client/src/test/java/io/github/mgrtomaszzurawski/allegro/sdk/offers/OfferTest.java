/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValuePublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.B2bRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemTextRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DiscountsProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ExternalIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LocationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRangeValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferAdditionalServicesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AiCoCreatedContentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AiCoCreatedImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferAttachmentInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferFundraisingCampaignResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StandardizedDescriptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionItemType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MarketplacePublicationState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDescription;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferMarketplace;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferLocation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ParameterRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.StockUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfferTest {

    private static final String OFFER_ID = "13579";
    private static final String MARKETPLACE_ID = "allegro-cz";
    private static final String MARKETPLACE_AMOUNT = "899.00";
    private static final String MARKETPLACE_CURRENCY = "CZK";
    private static final String ATTACHMENT_ID = "3f8b2c10-0000-4000-8000-000000000abc";
    private static final String ATTACHMENT_ID_2 = "5c7d1e20-0000-4000-8000-000000000def";
    private static final String AI_IMAGE_URL = "https://a.allegroimg.com/original/11ea99/ai-one";
    private static final String AI_IMAGE_URL_2 = "https://a.allegroimg.com/original/11ea99/ai-two";
    private static final String CATEGORY_ID = "257";
    private static final String TEST_UNKNOWN_FORMAT = "FUTURE_FORMAT";
    private static final String TEST_UNKNOWN_STATUS = "FUTURE_STATUS";
    private static final String SHIPPING_RATES_ID = "a1b2c3d4-0000-0000-0000-000000000001";
    private static final String HANDLING_TIME = "PT48H";
    private static final String IMPLIED_WARRANTY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String RETURN_POLICY_ID = "22222222-2222-2222-2222-222222222222";
    private static final String CURRENCY_PLN = "PLN";
    private static final String STARTING_AMOUNT = "1.00";
    private static final String MINIMAL_AMOUNT = "150.00";
    private static final String NAME_FULL = "Full";
    private static final String NAME_AUCTION = "Auction";
    private static final int AUCTION_STOCK = 3;
    private static final String DESC_TEXT_TYPE = "TEXT";
    private static final String DESC_CONTENT = "<p>Great keyboard</p>";
    private static final String CITY = "Warszawa";
    private static final String COUNTRY_CODE = "PL";
    private static final String PARAM_DICT_ID = "11321";
    private static final String PARAM_DICT_NAME = "Color";
    private static final String PARAM_DICT_VALUE_ID = "1";
    private static final String PARAM_DICT_LABEL = "Red";
    private static final String PARAM_RANGE_ID = "12345";
    private static final String PARAM_RANGE_FROM = "10";
    private static final String PARAM_RANGE_TO = "20";
    private static final int EXPECTED_PARAM_COUNT = 2;
    private static final String EXTERNAL_ID = "SKU-12345";
    private static final String LANGUAGE = "pl-PL";
    private static final String SIZE_TABLE_ID = "size-table-1";
    private static final String TAX_SUBJECT = "GOODS";
    private static final String TAX_EXEMPTION = "NONE";
    private static final String TAX_RATE = "23";
    private static final String TAX_COUNTRY = "PL";
    private static final String CONTACT_ID = "contact-1";
    private static final String ADDITIONAL_SERVICES_ID = "group-1";
    private static final String FUNDRAISING_ID = "campaign-1";
    private static final String WHOLESALE_PRICE_LIST_ID = "wholesale-1";

    @Test
    void from_whenFormatAndStatusAbsent_mapsBothToUnknown() {
        // given — a payload whose selling-mode format and publication status are
        // absent (an as-yet-unmodelled or partial state)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name("Partial")
                .category(new OfferCategoryRaw().id(CATEGORY_ID))
                .sellingMode(new SellingModeRaw())
                .publication(new SaleProductOfferPublicationResponseRaw());

        // when
        Offer offer = Offer.from(raw);

        // then — unknown enum values fall back rather than throwing, price/stock null
        assertEquals(OfferFormat.UNKNOWN, offer.format());
        assertEquals(OfferStatus.UNKNOWN, offer.status());
        assertNull(offer.buyNowPrice());
        assertNull(offer.availableStock());
    }

    @Test
    void from_whenFormatAndStatusCarryUnknownWireValue_degradesInsteadOfThrowing() {
        // given — Allegro returns a selling-mode format and a status value added
        // after this SDK release (a value the generated enums do not model)
        SellingModeFormatRaw unknownFormat =
                assertDoesNotThrow(() -> SellingModeFormatRaw.fromValue(TEST_UNKNOWN_FORMAT));
        OfferStatusRaw unknownStatus =
                assertDoesNotThrow(() -> OfferStatusRaw.fromValue(TEST_UNKNOWN_STATUS));

        // then — Layer 1 returns the forward-compat sentinel instead of throwing
        // (enumUnknownDefaultCase), and the domain mapping degrades it to UNKNOWN
        // rather than failing the whole response deserialization
        assertEquals(SellingModeFormatRaw.UNKNOWN_DEFAULT_OPEN_API, unknownFormat);
        assertEquals(OfferStatusRaw.UNKNOWN_DEFAULT_OPEN_API, unknownStatus);
        assertEquals(OfferFormat.UNKNOWN, OfferFormat.from(unknownFormat));
        assertEquals(OfferStatus.UNKNOWN, OfferStatus.from(unknownStatus));
    }

    @Test
    void from_whenNestedObjectsAbsent_toleratesWithoutNullPointer() {
        // given — a payload with no selling mode, category or publication at all
        // (the spec marks none of them required, so mapping must not dereference)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name("Bare");

        // when
        Offer offer = Offer.from(raw);

        // then — every optional projection degrades to null/UNKNOWN, no NPE
        assertEquals(OFFER_ID, offer.id());
        assertNull(offer.categoryId());
        assertEquals(OfferFormat.UNKNOWN, offer.format());
        assertEquals(OfferStatus.UNKNOWN, offer.status());
        assertNull(offer.buyNowPrice());
        assertNull(offer.availableStock());
        assertNull(offer.delivery());
        assertNull(offer.afterSalesServices());
        assertNull(offer.description());
        assertNull(offer.location());
        assertEquals(List.of(), offer.parameters());
        assertNull(offer.externalId());
        assertNull(offer.language());
        assertNull(offer.sizeTableId());
    }

    @Test
    void from_whenOfferRefsPresent_mapsExternalIdLanguageAndSizeTable() {
        // given — a payload carrying the seller external id, listing language and a size table
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .external(new ExternalIdRaw().id(EXTERNAL_ID))
                .language(LANGUAGE)
                .sizeTable(new SizeTableRaw().id(SIZE_TABLE_ID));

        // when
        Offer offer = Offer.from(raw);

        // then — each reference is unwrapped onto the flat consumer field
        assertEquals(EXTERNAL_ID, offer.externalId());
        assertEquals(LANGUAGE, offer.language());
        assertEquals(SIZE_TABLE_ID, offer.sizeTableId());
    }

    @Test
    void from_whenAdditionalMarketplacesPresent_projectsThemByMarketplaceId() {
        // given — the offer is cross-listed on a foreign marketplace with a price and state
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID).name(NAME_FULL)
                .additionalMarketplaces(Map.of(MARKETPLACE_ID, new AdditionalMarketplacesResponseValueRaw()
                        .sellingMode(new SellingModeRaw()
                                .format(SellingModeFormatRaw.BUY_NOW)
                                .price(new BuyNowPriceRaw().amount(MARKETPLACE_AMOUNT).currency(MARKETPLACE_CURRENCY)))
                        .publication(new AdditionalMarketplacesResponseValuePublicationRaw()
                                .state(AdditionalMarketplacesResponseValuePublicationRaw.StateEnum.APPROVED))));

        // when
        Offer offer = Offer.from(raw);

        // then — the per-marketplace value is keyed by marketplace id
        OfferMarketplace marketplace = offer.additionalMarketplaces().get(MARKETPLACE_ID);
        assertNotNull(marketplace);
        assertEquals(MARKETPLACE_AMOUNT, marketplace.price().amount());
        assertEquals(MarketplacePublicationState.APPROVED, marketplace.publicationState());
    }

    @Test
    void from_whenNoAdditionalMarketplaces_isEmpty() {
        // then — an offer that is not cross-listed exposes an empty map
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw().id(OFFER_ID).name(NAME_FULL);
        assertTrue(Offer.from(raw).additionalMarketplaces().isEmpty());
    }

    @Test
    void from_whenAttachmentsPresent_projectsTheirIds() {
        // given — an offer with a linked attachment
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID).name(NAME_FULL)
                .attachments(List.of(new ProductOfferAttachmentInnerRaw().id(ATTACHMENT_ID)));

        // then — the attachment ids are surfaced flat
        assertEquals(List.of(ATTACHMENT_ID), Offer.from(raw).attachmentIds());
    }

    @Test
    void from_whenNoAttachments_isEmpty() {
        // then — an offer with no linked attachments exposes an empty list
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw().id(OFFER_ID).name(NAME_FULL);
        assertTrue(Offer.from(raw).attachmentIds().isEmpty());
    }

    @Test
    void from_whenAttachmentHasNullId_skipsItAndKeepsOrder() {
        // given — attachments where one carries no id (spec-legal: id is not required)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID).name(NAME_FULL)
                .attachments(List.of(
                        new ProductOfferAttachmentInnerRaw().id(ATTACHMENT_ID),
                        new ProductOfferAttachmentInnerRaw(),
                        new ProductOfferAttachmentInnerRaw().id(ATTACHMENT_ID_2)));

        // then — the null id is dropped and the surviving ids keep their order
        assertEquals(List.of(ATTACHMENT_ID, ATTACHMENT_ID_2), Offer.from(raw).attachmentIds());
    }

    @Test
    void from_whenAiCoCreatedImagesPresent_readsUrlsAndSkipsNullUrlKeepingOrder() {
        // given — an offer whose aiCoCreatedContent lists two AI images plus one with no url
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID).name(NAME_FULL)
                .aiCoCreatedContent(new AiCoCreatedContentRaw().images(List.of(
                        new AiCoCreatedImageRaw().url(AI_IMAGE_URL),
                        new AiCoCreatedImageRaw(),
                        new AiCoCreatedImageRaw().url(AI_IMAGE_URL_2))));

        // then — the null-url entry is dropped and the surviving urls keep their order
        assertEquals(List.of(AI_IMAGE_URL, AI_IMAGE_URL_2), Offer.from(raw).aiCoCreatedImageUrls());
    }

    @Test
    void from_whenNoAiCoCreatedContent_isEmpty() {
        // then — an offer with no aiCoCreatedContent exposes an empty list
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw().id(OFFER_ID).name(NAME_FULL);
        assertTrue(Offer.from(raw).aiCoCreatedImageUrls().isEmpty());
    }

    @Test
    void from_whenAiCoCreatedContentHasNullImages_isEmpty() {
        // given — the block is present but its images list is null (spec-legal: images is optional)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID).name(NAME_FULL)
                .aiCoCreatedContent(new AiCoCreatedContentRaw().images(null));

        // then — an empty list, no NullPointerException
        assertTrue(Offer.from(raw).aiCoCreatedImageUrls().isEmpty());
    }

    @Test
    void from_whenBusinessOnlyPresent_unwrapsTheFlag() {
        // given — a payload restricting the offer to business buyers
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .b2b(new B2bRaw().buyableOnlyByBusiness(true));

        // when
        Offer offer = Offer.from(raw);

        // then — the nested flag is surfaced flat
        assertEquals(Boolean.TRUE, offer.businessOnly());
    }

    @Test
    void from_whenBusinessOnlyFalse_passesTheValueThrough() {
        // given — a payload explicitly NOT restricted to business buyers (proves the value is
        // passed through, not short-circuited to a constant)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .b2b(new B2bRaw().buyableOnlyByBusiness(false));

        // when
        Offer offer = Offer.from(raw);

        // then
        assertEquals(Boolean.FALSE, offer.businessOnly());
    }

    @Test
    void from_whenB2bBlockAbsent_leavesBusinessOnlyNull() {
        // given — a payload without a b2b block
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL);

        // when
        Offer offer = Offer.from(raw);

        // then
        assertNull(offer.businessOnly());
    }

    @Test
    void from_whenTaxSettingsPresent_mapsRatesSubjectAndExemption() {
        // given — a payload carrying VAT settings
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .taxSettings(new OfferTaxSettingsRaw()
                        .subject(TAX_SUBJECT)
                        .exemption(TAX_EXEMPTION)
                        .rates(List.of(new OfferTaxRateRaw().rate(TAX_RATE).countryCode(TAX_COUNTRY))));

        // when
        Offer offer = Offer.from(raw);

        // then
        assertEquals(TAX_SUBJECT, offer.taxSettings().subject());
        assertEquals(TAX_EXEMPTION, offer.taxSettings().exemption());
        assertEquals(TAX_RATE, offer.taxSettings().rates().get(0).rate());
        assertEquals(TAX_COUNTRY, offer.taxSettings().rates().get(0).countryCode());
    }

    @Test
    void from_whenTaxSettingsAbsent_leavesItNull() {
        // given — a payload without a tax-settings block
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL);

        // when
        Offer offer = Offer.from(raw);

        // then
        assertNull(offer.taxSettings());
    }

    @Test
    void from_whenReferencesPresent_unwrapsEachId() {
        // given — a payload carrying a contact, additional-services group and fundraising campaign
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .contact(new ContactRaw().id(CONTACT_ID))
                .additionalServices(new ProductOfferAdditionalServicesResponseRaw().id(ADDITIONAL_SERVICES_ID))
                .fundraisingCampaign(new ProductOfferFundraisingCampaignResponseRaw().id(FUNDRAISING_ID))
                .discounts(new DiscountsProductOfferResponseRaw().wholesalePriceList(
                        new JustIdRaw().id(WHOLESALE_PRICE_LIST_ID)));

        // when
        Offer offer = Offer.from(raw);

        // then — each nested id is surfaced flat
        assertEquals(CONTACT_ID, offer.contactId());
        assertEquals(ADDITIONAL_SERVICES_ID, offer.additionalServicesGroupId());
        assertEquals(FUNDRAISING_ID, offer.fundraisingCampaignId());
        assertEquals(WHOLESALE_PRICE_LIST_ID, offer.wholesalePriceListId());
    }

    @Test
    void from_whenReferencesAbsent_leaveThemNull() {
        // given — a payload without those reference blocks
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL);

        // when
        Offer offer = Offer.from(raw);

        // then
        assertNull(offer.contactId());
        assertNull(offer.additionalServicesGroupId());
        assertNull(offer.fundraisingCampaignId());
        assertNull(offer.wholesalePriceListId());
    }

    @Test
    void from_whenDiscountsPresentButWholesalePriceListAbsent_leavesIdNull() {
        // given — a discounts block present but without a wholesale price list (a real wire
        // shape: wholesalePriceList is JsonNullable). Guards the projector's middle null-branch.
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .discounts(new DiscountsProductOfferResponseRaw());

        // when
        Offer offer = Offer.from(raw);

        // then
        assertNull(offer.wholesalePriceListId());
    }

    @Test
    void from_whenParametersPresent_mapsDictionaryAndRangeInOrder() {
        // given — a payload with a dictionary parameter (value ids) and a range parameter
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .parameters(List.of(
                        new ParameterProductOfferResponseRaw()
                                .id(PARAM_DICT_ID).name(PARAM_DICT_NAME)
                                .values(List.of(PARAM_DICT_LABEL)).valuesIds(List.of(PARAM_DICT_VALUE_ID)),
                        new ParameterProductOfferResponseRaw()
                                .id(PARAM_RANGE_ID)
                                .rangeValue(new ParameterRangeValueRaw().from(PARAM_RANGE_FROM).to(PARAM_RANGE_TO))));

        // when
        Offer offer = Offer.from(raw);

        // then — both parameters project in order; the dictionary one carries BOTH its
        // ids and their labels (the real read shape), the range one carries its bounds
        List<OfferParameter> parameters = offer.parameters();
        assertEquals(EXPECTED_PARAM_COUNT, parameters.size());
        OfferParameter dictionary = parameters.get(0);
        assertEquals(PARAM_DICT_ID, dictionary.id());
        assertEquals(PARAM_DICT_NAME, dictionary.name());
        assertEquals(List.of(PARAM_DICT_VALUE_ID), dictionary.valuesIds());
        assertEquals(List.of(PARAM_DICT_LABEL), dictionary.values());
        assertNull(dictionary.rangeValue());
        OfferParameter range = parameters.get(1);
        assertEquals(PARAM_RANGE_ID, range.id());
        assertEquals(new ParameterRange(PARAM_RANGE_FROM, PARAM_RANGE_TO), range.rangeValue());
        assertEquals(List.of(), range.valuesIds());
    }

    @Test
    void from_whenDescriptionAndLocationPresent_mapsBoth() {
        // given — a payload with a one-section text description and a location
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .description(new StandardizedDescriptionRaw().sections(List.of(
                        new DescriptionSectionRaw().items(List.of(
                                new DescriptionSectionItemTextRaw().type(DESC_TEXT_TYPE).content(DESC_CONTENT))))))
                .location(new LocationRaw().city(CITY).countryCode(COUNTRY_CODE));

        // when
        Offer offer = Offer.from(raw);

        // then — the description text item and the location are projected
        OfferDescription description = offer.description();
        assertNotNull(description);
        DescriptionItem item = description.sections().get(0).items().get(0);
        assertEquals(DescriptionItemType.TEXT, item.type());
        assertEquals(DESC_CONTENT, item.content());
        assertEquals(new OfferLocation(CITY, COUNTRY_CODE, null, null), offer.location());
    }

    @Test
    void from_whenDeliveryAndAfterSalesPresent_mapsBothNestedBlocks() {
        // given — a payload carrying delivery terms and after-sales conditions
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_FULL)
                .delivery(new DeliveryProductOfferResponseRaw()
                        .shippingRates(new JustIdRaw().id(SHIPPING_RATES_ID))
                        .handlingTime(HANDLING_TIME))
                .afterSalesServices(new AfterSalesServicesRaw()
                        .impliedWarranty(new ImpliedWarrantyRaw().id(UUID.fromString(IMPLIED_WARRANTY_ID)))
                        .returnPolicy(new ReturnPolicyRaw().id(UUID.fromString(RETURN_POLICY_ID)))
                        .warranty(new WarrantyRaw()));

        // when
        Offer offer = Offer.from(raw);

        // then — both nested blocks are projected onto the consumer records
        OfferDelivery delivery = offer.delivery();
        assertNotNull(delivery);
        assertEquals(SHIPPING_RATES_ID, delivery.shippingRatesId());
        assertEquals(HANDLING_TIME, delivery.handlingTime());
        AfterSalesServices afterSales = offer.afterSalesServices();
        assertNotNull(afterSales);
        assertEquals(IMPLIED_WARRANTY_ID, afterSales.impliedWarranty().id());
        assertEquals(RETURN_POLICY_ID, afterSales.returnPolicy().id());
        assertNull(afterSales.warranty());
    }

    @Test
    void from_whenAuctionSellingMode_mapsStartingMinimalPriceAndStockUnit() {
        // given — an auction payload with a starting and minimal price and a PAIR unit
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name(NAME_AUCTION)
                .sellingMode(new SellingModeRaw()
                        .format(SellingModeFormatRaw.AUCTION)
                        .startingPrice(new StartingPriceRaw().amount(STARTING_AMOUNT).currency(CURRENCY_PLN))
                        .minimalPrice(new MinimalPriceRaw().amount(MINIMAL_AMOUNT).currency(CURRENCY_PLN)))
                .stock(new StockRaw().available(AUCTION_STOCK).unit(StockRaw.UnitEnum.PAIR));

        // when
        Offer offer = Offer.from(raw);

        // then — the auction prices and the stock unit are projected
        assertEquals(OfferFormat.AUCTION, offer.format());
        assertEquals(Money.of(STARTING_AMOUNT, CURRENCY_PLN), offer.startingPrice());
        assertEquals(Money.of(MINIMAL_AMOUNT, CURRENCY_PLN), offer.minimalPrice());
        assertNull(offer.buyNowPrice());
        assertEquals(StockUnit.PAIR, offer.stockUnit());
    }

    @Test
    void offerFormatToRaw_whenKnownValue_mapsToWireEnum() {
        // then — the writable formats map to their wire enum
        assertEquals(SellingModeFormatRaw.BUY_NOW, OfferFormat.BUY_NOW.toRaw());
        assertEquals(SellingModeFormatRaw.AUCTION, OfferFormat.AUCTION.toRaw());
        assertEquals(SellingModeFormatRaw.ADVERTISEMENT, OfferFormat.ADVERTISEMENT.toRaw());
    }

    @Test
    void offerFormatToRaw_whenUnknown_throwsBecauseSentinelIsReadOnly() {
        assertThrows(IllegalStateException.class, OfferFormat.UNKNOWN::toRaw);
    }
}
