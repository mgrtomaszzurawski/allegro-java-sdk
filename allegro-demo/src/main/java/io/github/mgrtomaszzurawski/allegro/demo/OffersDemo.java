/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.OfferMedia;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange.CurrencyBasis;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.PriceChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.StockChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.HandlingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferDuration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferPart;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PromoModificationTiming;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PublicationSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InlineProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.NamedReference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaxSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoOptionModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoPackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferProcessingStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.CompatibilityEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferImage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferLocation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPayments;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PartialOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceStockBatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductDeposit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductIdType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.UnfilledParameters;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ResponsiblePersonRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ResponsibleProducerRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SafetyInformation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bucket A sandbox probe. With no {@code -Pdemo.offerId} it lists the seller's
 * offers via the lazy {@code streamOffers} read; with an id it reads that offer
 * plus its Smart! classification, and — when {@code -Pdemo.newPrice} is also
 * given — runs the write→read cycle (change the Buy Now price through the SDK,
 * then read it back and confirm the round-trip).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer                        # list my offers
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.offerId=13579
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.offerId=13579 -Pdemo.newPrice=149.50
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.publishIds=13579,24680  # bulk publish
 * </pre>
 */
final class OffersDemo {

    private static final String OFFER_ID_PROPERTY = "demo.offerId";
    private static final String NEW_PRICE_PROPERTY = "demo.newPrice";
    private static final String PUBLISH_IDS_PROPERTY = "demo.publishIds";
    private static final String CREATE_NAME_PROPERTY = "demo.createName";
    private static final String CREATE_CATEGORY_PROPERTY = "demo.createCategory";
    private static final String CREATE_PRICE_PROPERTY = "demo.createPrice";
    private static final String CREATE_STOCK_PROPERTY = "demo.createStock";
    private static final String CREATE_PRODUCT_ID_PROPERTY = "demo.createProductId";
    private static final String CREATE_PRODUCT_ID_TYPE_PROPERTY = "demo.createProductIdType";
    private static final String CREATE_PRODUCER_ID_PROPERTY = "demo.createProducerId";
    private static final String CREATE_RESPONSIBLE_PERSON_ID_PROPERTY = "demo.createResponsiblePersonId";
    private static final String CREATE_SAFETY_TEXT_PROPERTY = "demo.createSafetyText";
    private static final String CREATE_DEPOSIT_ID_PROPERTY = "demo.createDepositId";
    private static final String CREATE_INLINE_PRODUCT_NAME_PROPERTY = "demo.createInlineProductName";
    private static final String CREATE_INLINE_PRODUCT_CATEGORY_PROPERTY = "demo.createInlineProductCategory";
    private static final String CREATE_QUANTITY_PROPERTY = "demo.createQuantity";
    private static final String CREATE_SHIPPING_RATES_ID_PROPERTY = "demo.createShippingRatesId";
    private static final String CREATE_PROVINCE_PROPERTY = "demo.createProvince";
    private static final String CREATE_CITY_PROPERTY = "demo.createCity";
    private static final String CREATE_POST_CODE_PROPERTY = "demo.createPostCode";
    private static final String CREATE_COUNTRY_CODE_PROPERTY = "demo.createCountryCode";
    private static final String CREATE_IMPLIED_WARRANTY_ID_PROPERTY = "demo.createImpliedWarrantyId";
    private static final String CREATE_RETURN_POLICY_ID_PROPERTY = "demo.createReturnPolicyId";
    private static final String CREATE_WARRANTY_ID_PROPERTY = "demo.createWarrantyId";
    private static final String CREATE_IMPLIED_WARRANTY_NAME_PROPERTY = "demo.createImpliedWarrantyName";
    private static final String CREATE_BUSINESS_ONLY_PROPERTY = "demo.createBusinessOnly";
    private static final String CREATE_REPUBLISH_PROPERTY = "demo.createRepublish";
    private static final String CREATE_TAX_RATE_PROPERTY = "demo.createTaxRate";
    private static final String CREATE_TAX_COUNTRY_PROPERTY = "demo.createTaxCountry";
    private static final String CREATE_CONTACT_ID_PROPERTY = "demo.createContactId";
    private static final String CREATE_CONTACT_NAME_PROPERTY = "demo.createContactName";
    private static final String CREATE_ADDITIONAL_SERVICES_ID_PROPERTY = "demo.createAdditionalServicesId";
    private static final String CREATE_FUNDRAISING_ID_PROPERTY = "demo.createFundraisingId";
    private static final String CREATE_WHOLESALE_PRICE_LIST_ID_PROPERTY = "demo.createWholesalePriceListId";
    private static final String CREATE_MESSAGE_MODE_PROPERTY = "demo.createMessageMode";
    private static final String CREATE_MESSAGE_HINT_PROPERTY = "demo.createMessageHint";
    private static final String CREATE_INVOICE_TYPE_PROPERTY = "demo.createInvoiceType";
    private static final String CREATE_MARKETPLACE_ID_PROPERTY = "demo.createMarketplaceId";
    private static final String CREATE_ATTACHMENT_ID_PROPERTY = "demo.createAttachmentId";
    private static final String CREATE_UPLOAD_ATTACHMENT_PROPERTY = "demo.createUploadAttachment";
    private static final String CREATE_MARKETPLACE_PRICE_PROPERTY = "demo.createMarketplacePrice";
    private static final String CREATE_MARKETPLACE_CURRENCY_PROPERTY = "demo.createMarketplaceCurrency";
    private static final String CREATE_COMPAT_TEXT_PROPERTY = "demo.createCompatText";
    private static final String CREATE_COMPAT_PRODUCT_ID_PROPERTY = "demo.createCompatProductId";
    private static final String CREATE_AI_IMAGE_URL_PROPERTY = "demo.createAiImageUrl";
    private static final String DELETE_AFTER_CREATE_PROPERTY = "demo.deleteAfterCreate";
    private static final String PROMO_MODIFY_OFFER_ID_PROPERTY = "demo.promoModifyOfferId";
    private static final String PROMO_MODIFY_BASE_PACKAGE_PROPERTY = "demo.promoModifyBasePackage";
    private static final String DEFAULT_COUNTRY_CODE = "PL";
    private static final String UPLOAD_IMAGE_URL_PROPERTY = "demo.uploadImageUrl";
    private static final String DECLARE_ATTACHMENT_PROPERTY = "demo.declareAttachment";
    private static final String STREAM_EVENTS_PROPERTY = "demo.streamEvents";
    private static final String PROMO_OPTIONS_PROPERTY = "demo.promoOptions";
    private static final String BULK_MODIFY_OFFER_ID_PROPERTY = "demo.bulkModifyOfferId";
    private static final String BULK_PRICE_PROPERTY = "demo.bulkPrice";
    private static final String BULK_STOCK_PROPERTY = "demo.bulkStock";
    private static final String PRICING_RULE_OFFER_IDS_PROPERTY = "demo.pricingRuleOfferIds";
    private static final String PRICING_RULE_ID_PROPERTY = "demo.pricingRuleId";
    private static final String PRICING_RULE_MARKETPLACE_PROPERTY = "demo.pricingRuleMarketplace";
    private static final String PRICING_RULE_MIN_PRICE_PROPERTY = "demo.pricingRuleMinPrice";
    private static final String PRICING_RULE_MAX_PRICE_PROPERTY = "demo.pricingRuleMaxPrice";
    private static final String MODIFY_OFFER_IDS_PROPERTY = "demo.modifyOfferIds";
    private static final String MODIFY_DURATION_PROPERTY = "demo.modifyDuration";
    private static final String MODIFY_UNLIMITED_PROPERTY = "demo.modifyUnlimited";
    private static final String MODIFY_HANDLING_TIME_PROPERTY = "demo.modifyHandlingTime";
    private static final String PROMO_BATCH_OFFER_IDS_PROPERTY = "demo.promoBatchOfferIds";
    private static final String PROMO_BATCH_BASE_PACKAGE_PROPERTY = "demo.promoBatchBasePackage";
    private static final String PROMO_BATCH_EXTRA_PACKAGE_PROPERTY = "demo.promoBatchExtraPackage";
    private static final String PROMO_BATCH_TIMING_PROPERTY = "demo.promoBatchTiming";
    private static final String PARTS_OFFER_ID_PROPERTY = "demo.partsOfferId";
    private static final String PARTS_INCLUDE_PROPERTY = "demo.partsInclude";
    private static final String QUANTITY_OFFER_IDS_PROPERTY = "demo.quantityOfferIds";
    private static final String QUANTITY_VALUE_PROPERTY = "demo.quantityValue";
    private static final String UNPUBLISH_IDS_PROPERTY = "demo.unpublishIds";
    private static final String UNFILLED_PARAMS_PROPERTY = "demo.unfilledParams";
    private static final String EDIT_OFFER_ID_PROPERTY = "demo.editOfferId";
    private static final String EDIT_NAME_PROPERTY = "demo.editName";
    private static final String EDIT_PRICE_PROPERTY = "demo.editPrice";
    private static final String EDIT_STOCK_PROPERTY = "demo.editStock";
    private static final String CHANGE_PRICES_OFFER_IDS_PROPERTY = "demo.changePricesOfferIds";
    private static final String CHANGE_PRICES_VALUE_PROPERTY = "demo.changePricesValue";
    private static final String AVAILABLE_PACKAGES_PROPERTY = "demo.availablePackages";
    private static final String PROMO_FOR_OFFER_ID_PROPERTY = "demo.promoForOfferId";
    private static final String DELETE_DRAFT_OFFER_ID_PROPERTY = "demo.deleteDraftOfferId";
    private static final String DEFAULT_PARTS = "STOCK,PRICE";
    private static final String DEFAULT_MARKETPLACE = "allegro-pl";
    /** A minimal well-formed PDF, so the attachment upload leg is exercised through the SDK. */
    private static final String MINIMAL_PDF =
            "%PDF-1.1\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\n"
            + "trailer<</Root 1 0 R>>\n%%EOF\n";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final int DEFAULT_PRODUCT_QUANTITY = 1;
    private static final String CURRENCY_PLN = "PLN";
    private static final String OFFER_ID_SEPARATOR = ",";
    private static final int STREAM_LIMIT = 10;
    private static final int SCHEDULE_DAYS_AHEAD = 7;
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private OffersDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println("(stored token expired - rerun auth-bootstrap)"),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            String offerId = System.getProperty(OFFER_ID_PROPERTY);
            String publishIds = System.getProperty(PUBLISH_IDS_PROPERTY);
            String createName = System.getProperty(CREATE_NAME_PROPERTY);
            String uploadImageUrl = System.getProperty(UPLOAD_IMAGE_URL_PROPERTY);
            String declareAttachment = System.getProperty(DECLARE_ATTACHMENT_PROPERTY);
            if (uploadImageUrl != null) {
                uploadImage(client, uploadImageUrl);
            } else if (declareAttachment != null) {
                attachmentFlow(client, declareAttachment);
            } else if (System.getProperty(STREAM_EVENTS_PROPERTY) != null) {
                streamEvents(client);
            } else if (System.getProperty(PROMO_OPTIONS_PROPERTY) != null) {
                promoOptions(client);
            } else if (System.getProperty(PROMO_MODIFY_OFFER_ID_PROPERTY) != null) {
                promoModify(client, System.getProperty(PROMO_MODIFY_OFFER_ID_PROPERTY));
            } else if (System.getProperty(BULK_MODIFY_OFFER_ID_PROPERTY) != null) {
                bulkModify(client, System.getProperty(BULK_MODIFY_OFFER_ID_PROPERTY));
            } else if (System.getProperty(PRICING_RULE_OFFER_IDS_PROPERTY) != null) {
                applyPricingRules(client, System.getProperty(PRICING_RULE_OFFER_IDS_PROPERTY));
            } else if (System.getProperty(MODIFY_OFFER_IDS_PROPERTY) != null) {
                modifyOffers(client, System.getProperty(MODIFY_OFFER_IDS_PROPERTY));
            } else if (System.getProperty(PROMO_BATCH_OFFER_IDS_PROPERTY) != null) {
                modifyPromoBatch(client, System.getProperty(PROMO_BATCH_OFFER_IDS_PROPERTY));
            } else if (System.getProperty(PARTS_OFFER_ID_PROPERTY) != null) {
                getOfferParts(client, System.getProperty(PARTS_OFFER_ID_PROPERTY));
            } else if (System.getProperty(EDIT_OFFER_ID_PROPERTY) != null) {
                editOffer(client, System.getProperty(EDIT_OFFER_ID_PROPERTY));
            } else if (System.getProperty(CHANGE_PRICES_OFFER_IDS_PROPERTY) != null) {
                changePricesBatch(client, System.getProperty(CHANGE_PRICES_OFFER_IDS_PROPERTY));
            } else if (System.getProperty(AVAILABLE_PACKAGES_PROPERTY) != null) {
                availablePackages(client);
            } else if (System.getProperty(PROMO_FOR_OFFER_ID_PROPERTY) != null) {
                promoForOffer(client, System.getProperty(PROMO_FOR_OFFER_ID_PROPERTY));
            } else if (System.getProperty(DELETE_DRAFT_OFFER_ID_PROPERTY) != null) {
                deleteDraft(client, System.getProperty(DELETE_DRAFT_OFFER_ID_PROPERTY));
            } else if (System.getProperty(QUANTITY_OFFER_IDS_PROPERTY) != null) {
                changeQuantities(client, System.getProperty(QUANTITY_OFFER_IDS_PROPERTY));
            } else if (System.getProperty(UNPUBLISH_IDS_PROPERTY) != null) {
                unpublishBatch(client, System.getProperty(UNPUBLISH_IDS_PROPERTY));
            } else if (System.getProperty(UNFILLED_PARAMS_PROPERTY) != null) {
                streamUnfilled(client);
            } else if (createName != null) {
                createOffer(client, createName);
            } else if (publishIds != null) {
                publishBatch(client, publishIds);
            } else if (offerId == null) {
                streamOffers(client);
            } else {
                printOffer("read", client.offers().get(offerId));
                printSmart(client, offerId);
                String newPrice = System.getProperty(NEW_PRICE_PROPERTY);
                if (newPrice != null) {
                    client.offers().changeBuyNowPrice(offerId, Money.of(newPrice, CURRENCY_PLN));
                    System.out.println("changeBuyNowPrice submitted: " + newPrice + " " + CURRENCY_PLN);
                    printOffer("read-back", client.offers().get(offerId));
                }
            }
            rotateToken(tokenStore, account, client);
        }
    }

    private static void streamOffers(AllegroClient client) {
        List<OfferSummary> firstOffers = client.offers().streamOffers(OfferFilter.all())
                .limit(STREAM_LIMIT).toList();
        System.out.println("streamOffers: first " + firstOffers.size() + " offer(s)");
        for (OfferSummary summary : firstOffers) {
            String price = summary.buyNowPrice() == null ? "(no Buy Now price)"
                    : summary.buyNowPrice().amount() + " " + summary.buyNowPrice().currency();
            NamedReference returnPolicyRef =
                    summary.afterSalesServices() == null ? null : summary.afterSalesServices().returnPolicy();
            String returnPolicy = returnPolicyRef == null ? "(none)" : returnPolicyRef.id();
            System.out.println("  id=" + summary.id() + ", status=" + summary.status()
                    + ", format=" + summary.format() + ", stock=" + summary.availableStock()
                    + ", buyNow=" + price + ", fulfillment=" + summary.fulfillment()
                    + ", publishedAt=" + summary.publishedAt() + ", endedAt=" + summary.endedAt()
                    + ", returnPolicy=" + returnPolicy);
        }
    }

    private static void createOffer(AllegroClient client, String name) {
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(name)
                .categoryId(System.getProperty(CREATE_CATEGORY_PROPERTY))
                .buyNowPrice(Money.of(System.getProperty(CREATE_PRICE_PROPERTY), CURRENCY_PLN))
                .availableStock(Integer.parseInt(System.getProperty(CREATE_STOCK_PROPERTY)));
        // With -Pdemo.createProductId the create carries a productSet (product binding +
        // optional GPSR producer): a productized-category create through the SDK, so the
        // server exercises the productSet wire mapping. Any business-rule rejection still
        // proves the productSet deserialized (a shape/JsonMapping 400 would not).
        String productId = System.getProperty(CREATE_PRODUCT_ID_PROPERTY);
        if (productId != null) {
            String quantityValue = System.getProperty(CREATE_QUANTITY_PROPERTY);
            int quantity = quantityValue == null
                    ? DEFAULT_PRODUCT_QUANTITY : Integer.parseInt(quantityValue);
            ProductSetElement element = ProductSetElement.of(productId, quantity);
            String producerId = System.getProperty(CREATE_PRODUCER_ID_PROPERTY);
            if (producerId != null) {
                element = element.withResponsibleProducer(ResponsibleProducerRef.byId(producerId))
                        .withMarketedBeforeGpsrObligation(false);
            }
            String productIdType = System.getProperty(CREATE_PRODUCT_ID_TYPE_PROPERTY);
            if (productIdType != null) {
                element = element.withIdType(ProductIdType.valueOf(productIdType));
            }
            String responsiblePersonId = System.getProperty(CREATE_RESPONSIBLE_PERSON_ID_PROPERTY);
            if (responsiblePersonId != null) {
                element = element.withResponsiblePerson(ResponsiblePersonRef.byId(responsiblePersonId));
            }
            String safetyText = System.getProperty(CREATE_SAFETY_TEXT_PROPERTY);
            if (safetyText != null) {
                element = element.withSafetyInformation(SafetyInformation.text(safetyText));
            }
            String depositId = System.getProperty(CREATE_DEPOSIT_ID_PROPERTY);
            if (depositId != null) {
                element = element.withDeposits(List.of(ProductDeposit.of(depositId)));
            }
            String inlineProductName = System.getProperty(CREATE_INLINE_PRODUCT_NAME_PROPERTY);
            String inlineProductCategory = System.getProperty(CREATE_INLINE_PRODUCT_CATEGORY_PROPERTY);
            if (inlineProductName != null || inlineProductCategory != null) {
                element = element.withInlineProduct(InlineProduct.builder()
                        .name(inlineProductName).categoryId(inlineProductCategory).build());
            }
            builder.addProductSetElement(element);
            System.out.println("create: sending productSet product=" + productId
                    + " quantity=" + quantity + (producerId == null ? "" : " producer=" + producerId));
        }
        // Optional prerequisites a productized category needs to be created ACTIVE: a normal
        // (non-fulfillment) shipping rate, a ship-from location, and after-sales conditions.
        // Supplied via -Pdemo.create* so the SDK create path exercises the full wire mapping.
        String shippingRatesId = System.getProperty(CREATE_SHIPPING_RATES_ID_PROPERTY);
        if (shippingRatesId != null) {
            builder.delivery(OfferDelivery.builder().shippingRatesId(shippingRatesId).build());
        }
        String province = System.getProperty(CREATE_PROVINCE_PROPERTY);
        if (province != null) {
            builder.location(OfferLocation.builder()
                    .countryCode(System.getProperty(CREATE_COUNTRY_CODE_PROPERTY, DEFAULT_COUNTRY_CODE))
                    .province(province)
                    .city(System.getProperty(CREATE_CITY_PROPERTY))
                    .postCode(System.getProperty(CREATE_POST_CODE_PROPERTY))
                    .build());
        }
        NamedReference impliedWarranty = afterSalesRef(
                CREATE_IMPLIED_WARRANTY_ID_PROPERTY, CREATE_IMPLIED_WARRANTY_NAME_PROPERTY);
        NamedReference returnPolicy = afterSalesRef(CREATE_RETURN_POLICY_ID_PROPERTY, null);
        NamedReference warranty = afterSalesRef(CREATE_WARRANTY_ID_PROPERTY, null);
        if (impliedWarranty != null || returnPolicy != null || warranty != null) {
            builder.afterSalesServices(AfterSalesServices.builder()
                    .impliedWarranty(impliedWarranty)
                    .returnPolicy(returnPolicy)
                    .warranty(warranty)
                    .build());
        }
        if (System.getProperty(CREATE_BUSINESS_ONLY_PROPERTY) != null) {
            builder.businessOnly(Boolean.TRUE);
        }
        if (System.getProperty(CREATE_REPUBLISH_PROPERTY) != null) {
            // A scheduled publication keeps the offer INACTIVE until startingAt, so the draft
            // stays deletable — this lets the probe live-verify both republish and startingAt.
            builder.publication(PublicationSettings.builder()
                    .republish(Boolean.TRUE)
                    .startingAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(SCHEDULE_DAYS_AHEAD))
                    .build());
        }
        String taxRate = System.getProperty(CREATE_TAX_RATE_PROPERTY);
        if (taxRate != null) {
            String taxCountry = System.getProperty(CREATE_TAX_COUNTRY_PROPERTY, DEFAULT_COUNTRY_CODE);
            builder.taxSettings(TaxSettings.builder()
                    .rates(List.of(TaxRate.of(taxRate, taxCountry))).build());
        }
        applyIfPresent(CREATE_CONTACT_ID_PROPERTY, value -> builder.contact(NamedReference.byId(value)));
        applyIfPresent(CREATE_CONTACT_NAME_PROPERTY, value -> builder.contact(NamedReference.byName(value)));
        applyIfPresent(CREATE_ADDITIONAL_SERVICES_ID_PROPERTY,
                value -> builder.additionalServices(NamedReference.byId(value)));
        applyIfPresent(CREATE_FUNDRAISING_ID_PROPERTY,
                value -> builder.fundraisingCampaign(NamedReference.byId(value)));
        applyIfPresent(CREATE_WHOLESALE_PRICE_LIST_ID_PROPERTY,
                value -> builder.wholesalePriceList(NamedReference.byId(value)));
        applyIfPresent(CREATE_ATTACHMENT_ID_PROPERTY, value -> builder.attachmentIds(List.of(value)));
        String uploadAttachmentName = System.getProperty(CREATE_UPLOAD_ATTACHMENT_PROPERTY);
        if (uploadAttachmentName != null) {
            builder.attachmentIds(List.of(uploadAttachmentAndGetId(client, uploadAttachmentName)));
        }
        String messageMode = System.getProperty(CREATE_MESSAGE_MODE_PROPERTY);
        if (messageMode != null) {
            MessageToSellerMode mode = MessageToSellerMode.valueOf(messageMode);
            String messageHint = System.getProperty(CREATE_MESSAGE_HINT_PROPERTY);
            builder.messageToSellerSettings(messageHint == null
                    ? MessageToSellerSettings.of(mode)
                    : MessageToSellerSettings.of(mode, messageHint));
        }
        String invoiceType = System.getProperty(CREATE_INVOICE_TYPE_PROPERTY);
        if (invoiceType != null) {
            builder.payments(OfferPayments.of(InvoiceType.valueOf(invoiceType)));
        }
        String marketplaceId = System.getProperty(CREATE_MARKETPLACE_ID_PROPERTY);
        String marketplacePrice = System.getProperty(CREATE_MARKETPLACE_PRICE_PROPERTY);
        if (marketplaceId != null && marketplacePrice != null) {
            String marketplaceCurrency = System.getProperty(CREATE_MARKETPLACE_CURRENCY_PROPERTY, CURRENCY_PLN);
            builder.additionalMarketplacePrice(marketplaceId, Money.of(marketplacePrice, marketplaceCurrency));
        }
        applyIfPresent(CREATE_COMPAT_TEXT_PROPERTY,
                value -> builder.addCompatibilityEntry(CompatibilityEntry.text(value)));
        applyIfPresent(CREATE_COMPAT_PRODUCT_ID_PROPERTY,
                value -> builder.addCompatibilityEntry(CompatibilityEntry.productId(value)));
        applyIfPresent(CREATE_AI_IMAGE_URL_PROPERTY,
                value -> builder.aiCoCreatedImageUrls(List.of(value)));
        CreateOfferRequest request = builder.build();
        try {
            Offer created = client.offers().create(request);
            if (System.getProperty(CREATE_BUSINESS_ONLY_PROPERTY) != null) {
                System.out.println("create b2b businessOnly=" + created.businessOnly());
            }
            if (System.getProperty(CREATE_REPUBLISH_PROPERTY) != null && created.publication() != null) {
                System.out.println("create publication: status=" + created.status()
                        + ", republish=" + created.publication().republish()
                        + ", startingAt=" + created.publication().startingAt());
            }
            if (taxRate != null && created.taxSettings() != null) {
                System.out.println("create taxSettings: subject=" + created.taxSettings().subject()
                        + ", exemption=" + created.taxSettings().exemption()
                        + ", rates=" + created.taxSettings().rates());
            }
            if (messageMode != null && created.messageToSellerSettings() != null) {
                System.out.println("create messageToSeller: mode=" + created.messageToSellerSettings().mode()
                        + ", hint=" + created.messageToSellerSettings().hint());
            }
            if (invoiceType != null && created.payments() != null) {
                System.out.println("create payments: invoice=" + created.payments().invoice());
            }
            if (marketplaceId != null) {
                System.out.println("create additionalMarketplaces: " + created.additionalMarketplaces());
            }
            if (!created.attachmentIds().isEmpty()) {
                System.out.println("create attachmentIds: " + created.attachmentIds());
            }
            System.out.println("create: id=" + created.id() + ", status=" + created.status()
                    + ", name=" + created.name() + ", productSet=" + created.productSet().size());
            if (System.getProperty(CREATE_RESPONSIBLE_PERSON_ID_PROPERTY) != null
                    && !created.productSet().isEmpty()) {
                System.out.println("create responsiblePerson: "
                        + created.productSet().get(0).responsiblePerson());
            }
            if (System.getProperty(CREATE_SAFETY_TEXT_PROPERTY) != null
                    && !created.productSet().isEmpty()) {
                System.out.println("create safetyInformation: "
                        + created.productSet().get(0).safetyInformation());
            }
            if (System.getProperty(CREATE_DEPOSIT_ID_PROPERTY) != null
                    && !created.productSet().isEmpty()) {
                System.out.println("create deposits: " + created.productSet().get(0).deposits());
            }
            if (created.validation() != null) {
                System.out.println("create validation: " + created.validation().errors().size()
                        + " error(s), " + created.validation().warnings().size() + " warning(s)"
                        + (created.validation().warnings().isEmpty() ? ""
                                : ", e.g. " + created.validation().warnings().get(0).code()));
            }
            // The async create carries its operation id (from the Location header); poll the
            // processing status through the SDK to prove operationStatus(id, operationId) is usable.
            if (created.operationId() != null) {
                OfferProcessingStatus processing =
                        client.offers().operationStatus(created.id(), created.operationId());
                System.out.println("operationStatus: offer=" + processing.offerId()
                        + ", operation=" + processing.operationId() + ", status=" + processing.status());
            }
            // Delete-after-create: a just-created product offer is still an INACTIVE draft (the
            // sandbox auto-activates a valid one shortly after), so deleting it immediately
            // exercises the deleteDraft (DELETE /sale/offers/{id}) happy path through the SDK.
            if (System.getProperty(DELETE_AFTER_CREATE_PROPERTY) != null) {
                client.offers().deleteDraft(created.id());
                System.out.println("deleteDraft: " + created.id() + " deleted");
                try {
                    client.offers().get(created.id());
                    System.out.println("deleteDraft verify: UNEXPECTED — offer still readable");
                } catch (AllegroNotFoundException notFound) {
                    System.out.println("deleteDraft verify: offer is gone (404) — draft delete confirmed");
                }
            }
        } catch (AllegroBadRequestException e) {
            System.out.println("create rejected — " + e.errors().size() + " field error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    /** Apply a create-request setter from an optional {@code -Pdemo.*} system property. */
    private static void applyIfPresent(String property, Consumer<String> setter) {
        String value = System.getProperty(property);
        if (value != null) {
            setter.accept(value);
        }
    }

    /** Build a reference from an optional id property, else an optional name property, else null. */
    private static NamedReference afterSalesRef(String idProperty, String nameProperty) {
        String id = System.getProperty(idProperty);
        if (id != null) {
            return NamedReference.byId(id);
        }
        String name = nameProperty == null ? null : System.getProperty(nameProperty);
        return name == null ? null : NamedReference.byName(name);
    }

    private static void uploadImage(AllegroClient client, String imageUrl) {
        try {
            OfferImage image = client.offers().media().uploadImage(imageUrl);
            System.out.println("uploadImage: location=" + image.location()
                    + ", expiresAt=" + image.expiresAt());
        } catch (AllegroBadRequestException e) {
            System.out.println("uploadImage rejected — " + e.errors().size() + " error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - code=" + fieldError.code()
                    + " userMessage=" + fieldError.userMessage()));
        }
    }

    private static void promoModify(AllegroClient client, String offerId) {
        String basePackage = System.getProperty(PROMO_MODIFY_BASE_PACKAGE_PROPERTY);
        client.offers().promoOptions().modify(offerId,
                List.of(PromoOptionModification.change(PromoPackageType.BASE, basePackage)));
        System.out.println("promoOptions.modify: offer " + offerId + " base package set to " + basePackage);
        OfferPromoOptions after = client.offers().promoOptions().forOffer(offerId);
        System.out.println("  read-back: base="
                + (after.basePackage() == null ? "(none/pending)" : after.basePackage().id())
                + ", extras=" + after.extraPackages().size());
    }

    private static void promoOptions(AllegroClient client) {
        var promos = client.offers().promoOptions().forAllOffers().limit(STREAM_LIMIT).toList();
        System.out.println("promoOptions.forAllOffers: first " + promos.size() + " offer(s)");
        promos.forEach(promo -> System.out.println("  offerId=" + promo.offerId()
                + ", base=" + (promo.basePackage() == null ? "(none)" : promo.basePackage().id())
                + ", extras=" + promo.extraPackages().size()));
    }

    private static void streamEvents(AllegroClient client) {
        List<OfferEvent> events = client.offers().streamEvents(OfferEventFilter.all())
                .limit(STREAM_LIMIT).toList();
        System.out.println("streamEvents: first " + events.size() + " event(s)");
        for (OfferEvent event : events) {
            System.out.println("  id=" + event.id() + ", type=" + event.type()
                    + ", offerId=" + event.offerId() + ", occurredAt=" + event.occurredAt());
        }
    }

    private static void changeQuantities(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        int quantity = Integer.parseInt(System.getProperty(QUANTITY_VALUE_PROPERTY));
        BatchReport report = client.offers().batch().changeQuantities(offerIds, quantity);
        System.out.println("batch changeQuantities to " + quantity + ": " + report.success()
                + "/" + report.total() + " ok, " + report.failed() + " failed");
        report.tasks().forEach(task -> System.out.println("  offerId=" + task.offerId()
                + ", status=" + task.status()));
    }

    private static void unpublishBatch(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        BatchReport report = client.offers().batch().unpublish(offerIds);
        System.out.println("batch unpublish: " + report.success() + "/" + report.total()
                + " ok, " + report.failed() + " failed");
    }

    private static void streamUnfilled(AllegroClient client) {
        List<UnfilledParameters> unfilled = client.offers().streamUnfilledParameters()
                .limit(STREAM_LIMIT).toList();
        System.out.println("streamUnfilledParameters: first " + unfilled.size() + " offer(s)");
        unfilled.forEach(entry -> System.out.println("  " + entry));
    }

    private static void editOffer(AllegroClient client, String offerId) {
        EditOfferRequest.Builder builder = EditOfferRequest.builder();
        String newName = System.getProperty(EDIT_NAME_PROPERTY);
        if (newName != null) {
            builder.name(newName);
        }
        String newPrice = System.getProperty(EDIT_PRICE_PROPERTY);
        if (newPrice != null) {
            builder.buyNowPrice(Money.of(newPrice, CURRENCY_PLN));
        }
        // The server requires the stock module in a product-offer PATCH even when only the
        // price/name changes (RequiredModulesEnabled), so allow the demo to carry it.
        String newStock = System.getProperty(EDIT_STOCK_PROPERTY);
        if (newStock != null) {
            builder.availableStock(Integer.valueOf(newStock));
        }
        try {
            Offer edited = client.offers().edit(offerId, builder.build());
            System.out.println("edit: id=" + edited.id() + ", name=" + edited.name()
                    + ", buyNow=" + edited.buyNowPrice());
        } catch (AllegroBadRequestException e) {
            System.out.println("edit rejected — " + e.errors().size() + " field error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    private static void changePricesBatch(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        Money price = Money.of(System.getProperty(CHANGE_PRICES_VALUE_PROPERTY), CURRENCY_PLN);
        BatchReport report = client.offers().batch().changePrices(offerIds, price);
        System.out.println("batch changePrices to " + price.amount() + " " + price.currency() + ": "
                + report.success() + "/" + report.total() + " ok, " + report.failed() + " failed");
    }

    private static void availablePackages(AllegroClient client) {
        AvailablePromotionPackages packages = client.offers().promoOptions().availablePackages();
        System.out.println("availablePackages: base=" + packages.basePackages().size()
                + ", extra=" + packages.extraPackages().size());
    }

    private static void promoForOffer(AllegroClient client, String offerId) {
        OfferPromoOptions promo = client.offers().promoOptions().forOffer(offerId);
        System.out.println("forOffer " + offerId + ": base="
                + (promo.basePackage() == null ? "(none)" : promo.basePackage().id())
                + ", extras=" + promo.extraPackages().size());
    }

    private static void deleteDraft(AllegroClient client, String offerId) {
        client.offers().deleteDraft(offerId);
        System.out.println("deleteDraft: " + offerId + " deleted");
    }

    /** Declare + upload a minimal attachment and return its id, for linking on a create. */
    private static String uploadAttachmentAndGetId(AllegroClient client, String fileName) {
        OfferMedia media = client.offers().media();
        OfferAttachment declared = media.createAttachment(
                AttachmentDeclaration.of(AttachmentType.USER_MANUAL, fileName));
        media.uploadAttachment(declared, MINIMAL_PDF.getBytes(StandardCharsets.UTF_8), PDF_CONTENT_TYPE);
        System.out.println("uploadAttachment: id=" + declared.id() + " uploaded");
        return declared.id();
    }

    private static void attachmentFlow(AllegroClient client, String fileName) {
        OfferMedia media = client.offers().media();
        try {
            OfferAttachment declared = media.createAttachment(
                    AttachmentDeclaration.of(AttachmentType.USER_MANUAL, fileName));
            System.out.println("createAttachment: id=" + declared.id()
                    + ", uploadUrl=" + declared.uploadUrl());
            OfferAttachment uploaded = media.uploadAttachment(declared,
                    MINIMAL_PDF.getBytes(StandardCharsets.UTF_8), PDF_CONTENT_TYPE);
            System.out.println("uploadAttachment: fileUrl=" + uploaded.fileUrl());
            OfferAttachment read = media.getAttachment(declared.id());
            System.out.println("getAttachment: id=" + read.id() + ", type=" + read.type()
                    + ", fileName=" + read.fileName() + ", fileUrl=" + read.fileUrl());
        } catch (AllegroBadRequestException e) {
            System.out.println("attachment flow rejected — " + e.errors().size() + " error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    private static void publishBatch(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        BatchReport report = client.offers().batch().publish(offerIds);
        System.out.println("batch publish: " + report.success() + "/" + report.total()
                + " ok, " + report.failed() + " failed");
    }

    /**
     * Write→read of the bulk price/stock command: read the offer, submit a
     * {@code modifyPricesAndStock} (a fixed marketplace price and/or fixed stock
     * on {@code -Pdemo.bulkPrice}/{@code -Pdemo.bulkStock}), then read it back and
     * show the new price/stock so the round-trip is confirmed through the SDK.
     */
    private static void bulkModify(AllegroClient client, String offerId) {
        printOfferPriceStockIfPresent("before", client, offerId);
        BulkPriceStockModification.Builder builder = BulkPriceStockModification.forOffer(offerId);
        String price = System.getProperty(BULK_PRICE_PROPERTY);
        if (price != null) {
            builder.price(DEFAULT_MARKETPLACE, PriceChange.fixed(Money.of(price, CURRENCY_PLN)));
        }
        String stock = System.getProperty(BULK_STOCK_PROPERTY);
        if (stock != null) {
            builder.stock(StockChange.fixed(Integer.parseInt(stock)));
        }
        try {
            PriceStockBatchReport report = client.offers().batch()
                    .modifyPricesAndStock(List.of(builder.build()));
            System.out.println("modifyPricesAndStock: " + report.success() + "/" + report.total()
                    + " ok, " + report.failed() + " failed");
            report.tasks().forEach(task -> System.out.println("  offerId=" + task.offerId()
                    + ", field=" + task.field() + ", status=" + task.status()
                    + (task.message() == null ? "" : ", message=" + task.message())));
            printOfferPriceStockIfPresent("read-back", client, offerId);
        } catch (AllegroBadRequestException e) {
            System.out.println("modifyPricesAndStock rejected — " + e.errors().size() + " error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    /**
     * Submit the automatic-pricing command: assign a rule (with an optional price
     * range) to the given offers on a marketplace, or — with no
     * {@code -Pdemo.pricingRuleId} — remove the rules on that marketplace. The SDK
     * submits, polls to a terminal state and gathers the per-offer tasks, so the
     * printed report proves the whole command wire path; a typed rejection still
     * proves the request body deserialized server-side.
     */
    private static void applyPricingRules(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        String marketplace = System.getProperty(PRICING_RULE_MARKETPLACE_PROPERTY, DEFAULT_MARKETPLACE);
        String ruleId = System.getProperty(PRICING_RULE_ID_PROPERTY);
        BatchPricingRulesRequest request;
        if (ruleId == null) {
            request = BatchPricingRulesRequest.removeRules(offerIds).fromMarketplace(marketplace).build();
            System.out.println("applyPricingRules: removing rules on " + marketplace
                    + " from " + offerIds.size() + " offer(s)");
        } else {
            request = assignRuleRequest(offerIds, marketplace, ruleId);
            System.out.println("applyPricingRules: assigning rule " + ruleId + " on " + marketplace
                    + " to " + offerIds.size() + " offer(s)");
        }
        try {
            BatchReport report = client.offers().batch().applyPricingRules(request);
            System.out.println("applyPricingRules: " + report.success() + "/" + report.total()
                    + " ok, " + report.failed() + " failed");
            report.tasks().forEach(task -> System.out.println("  offerId=" + task.offerId()
                    + ", status=" + task.status()
                    + (task.message() == null ? "" : ", message=" + task.message())));
        } catch (AllegroBadRequestException e) {
            System.out.println("applyPricingRules rejected — " + e.errors().size() + " error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    /**
     * Partial-offer read: {@code getFields(offerId, OfferPart...)} retrieves only the
     * requested parts ({@code -Pdemo.partsInclude=STOCK,PRICE}, default both) — faster
     * and lighter than the full offer read.
     */
    private static void getOfferParts(AllegroClient client, String offerId) {
        OfferPart[] parts = Arrays.stream(
                        System.getProperty(PARTS_INCLUDE_PROPERTY, DEFAULT_PARTS).split(OFFER_ID_SEPARATOR))
                .map(String::trim).map(OfferPart::valueOf).toArray(OfferPart[]::new);
        try {
            PartialOffer partial = client.offers().getFields(offerId, parts);
            String price = partial.price() == null ? "(not requested)"
                    : partial.price().amount() + " " + partial.price().currency();
            System.out.println("getFields: id=" + partial.id() + ", stock=" + partial.availableStock()
                    + ", price=" + price + ", marketplacePrices=" + partial.marketplacePrices());
        } catch (AllegroNotFoundException e) {
            System.out.println("getFields: offer " + offerId + " not found");
        }
    }

    /**
     * Submit the batch promotion-package command: set a base package
     * ({@code -Pdemo.promoBatchBasePackage=emphasized1d}) and/or an extra package
     * ({@code -Pdemo.promoBatchExtraPackage=bold30d}), timed with
     * {@code -Pdemo.promoBatchTiming=NOW|END_OF_CYCLE}. The SDK submits, polls to a
     * terminal state (on task count — there is no completedAt) and gathers the
     * per-offer tasks, so the printed report proves the whole command wire path.
     */
    private static void modifyPromoBatch(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        BatchPromoOptionsRequest.Builder builder = BatchPromoOptionsRequest.forOffers(offerIds);
        String basePackage = System.getProperty(PROMO_BATCH_BASE_PACKAGE_PROPERTY);
        if (basePackage != null) {
            builder.basePackage(basePackage);
        }
        String extraPackage = System.getProperty(PROMO_BATCH_EXTRA_PACKAGE_PROPERTY);
        if (extraPackage != null) {
            builder.addExtraPackage(extraPackage);
        }
        String timing = System.getProperty(PROMO_BATCH_TIMING_PROPERTY);
        if (timing != null) {
            builder.timing(PromoModificationTiming.valueOf(timing));
        }
        System.out.println("promoBatch: applying packages to " + offerIds.size() + " offer(s)");
        try {
            BatchReport report = client.offers().promoOptions().modifyBatch(builder.build());
            System.out.println("promoBatch: " + report.success() + "/" + report.total()
                    + " ok, " + report.failed() + " failed");
            report.tasks().forEach(task -> System.out.println("  offerId=" + task.offerId()
                    + ", status=" + task.status()
                    + (task.message() == null ? "" : ", message=" + task.message())));
        } catch (AllegroBadRequestException e) {
            System.out.println("promoBatch rejected — " + e.errors().size() + " error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    /**
     * Submit the offer-modification command: set EXACTLY ONE setting on the given
     * offers — an unlimited listing ({@code -Pdemo.modifyUnlimited}), a fixed listing
     * duration ({@code -Pdemo.modifyDuration=DAYS_30}) or a dispatch time
     * ({@code -Pdemo.modifyHandlingTime=DAYS_2}), in that precedence (Allegro rejects a
     * command with more than one element). The SDK submits, polls to a terminal state
     * and gathers the per-offer tasks, so the printed report proves the command wire path.
     */
    private static void modifyOffers(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(offerIds);
        String duration = System.getProperty(MODIFY_DURATION_PROPERTY);
        String handlingTime = System.getProperty(MODIFY_HANDLING_TIME_PROPERTY);
        if (System.getProperty(MODIFY_UNLIMITED_PROPERTY) != null) {
            builder.unlimitedListing();
        } else if (duration != null) {
            builder.listingDuration(OfferDuration.valueOf(duration));
        } else if (handlingTime != null) {
            builder.handlingTime(HandlingTime.valueOf(handlingTime));
        }
        System.out.println("modify: applying a setting to " + offerIds.size() + " offer(s)");
        try {
            BatchReport report = client.offers().batch().modify(builder.build());
            System.out.println("modify: " + report.success() + "/" + report.total()
                    + " ok, " + report.failed() + " failed");
            report.tasks().forEach(task -> System.out.println("  offerId=" + task.offerId()
                    + ", status=" + task.status()
                    + (task.message() == null ? "" : ", message=" + task.message())));
        } catch (AllegroBadRequestException e) {
            System.out.println("modify rejected — " + e.errors().size() + " error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    private static BatchPricingRulesRequest assignRuleRequest(List<String> offerIds,
            String marketplace, String ruleId) {
        BatchPricingRulesRequest.AssignBuilder assign = BatchPricingRulesRequest.assignRules(offerIds);
        String minPrice = System.getProperty(PRICING_RULE_MIN_PRICE_PROPERTY);
        String maxPrice = System.getProperty(PRICING_RULE_MAX_PRICE_PROPERTY);
        if (minPrice != null && maxPrice != null) {
            assign.onMarketplace(marketplace, ruleId, PriceRange.of(CurrencyBasis.MARKETPLACE_CURRENCY,
                    Money.of(minPrice, CURRENCY_PLN), Money.of(maxPrice, CURRENCY_PLN)));
        } else {
            assign.onMarketplace(marketplace, ruleId);
        }
        return assign.build();
    }

    private static void printOfferPriceStockIfPresent(String phase, AllegroClient client, String offerId) {
        try {
            printOfferPriceStock(phase, client.offers().get(offerId));
        } catch (AllegroNotFoundException e) {
            System.out.println(phase + ": offer " + offerId
                    + " not found (probing the command wire shape only)");
        }
    }

    private static void printOfferPriceStock(String phase, Offer offer) {
        String price = offer.buyNowPrice() == null ? "(no Buy Now price)"
                : offer.buyNowPrice().amount() + " " + offer.buyNowPrice().currency();
        System.out.println(phase + ": id=" + offer.id() + ", buyNow=" + price
                + ", stock=" + offer.availableStock());
    }

    private static void printSmart(AllegroClient client, String offerId) {
        SmartClassification smart = client.offers().smartClassification(offerId);
        System.out.println("smart: fulfilled=" + smart.fulfilled()
                + ", scheduledForReclassification=" + smart.scheduledForReclassification()
                + ", conditions=" + smart.conditions().size());
    }

    private static void printOffer(String phase, Offer offer) {
        String price = offer.buyNowPrice() == null ? "(no Buy Now price)"
                : offer.buyNowPrice().amount() + " " + offer.buyNowPrice().currency();
        System.out.println(phase + ": id=" + offer.id() + ", status=" + offer.status()
                + ", format=" + offer.format() + ", buyNow=" + price);
        if (!offer.productSet().isEmpty()) {
            ProductSetElement element = offer.productSet().get(0);
            String params = element.productParameters().stream()
                    .map(parameter -> parameter.name() + "=" + parameter.values())
                    .toList().toString();
            String safety = element.safetyInformation() == null ? "(none)"
                    : element.safetyInformation().type();
            System.out.println("  productSet[0]: product=" + element.productId()
                    + ", aiCoCreated=" + element.aiCoCreated() + ", safety=" + safety
                    + ", parameters=" + params);
        }
    }

    private static void rotateToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotated = client.refreshToken();
        if (rotated != null) {
            tokenStore.store(account, rotated);
        }
    }
}
