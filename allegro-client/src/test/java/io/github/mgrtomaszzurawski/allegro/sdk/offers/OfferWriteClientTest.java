/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDescription;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferLocation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ResponsibleProducerRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.StockUnit;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OffersImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferWriteClientTest {

    private static final String TEST_TOKEN = "offers-write-token";
    private static final String CREATE_PATH = "/sale/product-offers";
    private static final String CREATED_OFFER_ID = "13579";
    private static final String CREATE_OPERATION_ID = "c12370ae-1612-48d9-9c3f-34e4d944c2c7";
    private static final String CREATE_LOCATION =
            "https://api.allegro.pl/sale/product-offers/" + CREATED_OFFER_ID
                    + "/operations/" + CREATE_OPERATION_ID;
    private static final String DELETE_OFFER_ID = "97531";
    private static final String DELETE_PATH = "/sale/offers/" + DELETE_OFFER_ID;
    private static final String EDIT_OFFER_ID = "654321";
    private static final String EDIT_PATH = "/sale/product-offers/" + EDIT_OFFER_ID;
    private static final String EDIT_NAME = "Renamed keyboard";
    private static final int EDIT_STOCK = 25;
    private static final String OFFER_FIXTURE = "offers/product-offer.json";

    private static final String NAME = "Mechanical keyboard";
    private static final String CATEGORY_ID = "257";
    private static final String AMOUNT = "199.99";
    private static final String CURRENCY_PLN = "PLN";
    private static final int STOCK = 10;

    private static final String PRICE_JSON_PATH = "$.sellingMode.price.amount";
    private static final String IMAGES_JSON_PATH = "$.images[0]";
    private static final String IMAGE_URL = "https://img.example/keyboard.jpg";

    private static final String SHIPPING_RATES_ID = "a1b2c3d4-0000-0000-0000-000000000001";
    private static final String HANDLING_TIME = "PT24H";
    private static final String DELIVERY_INFO = "Ships from Warsaw";
    private static final String IMPLIED_WARRANTY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String RETURN_POLICY_ID = "22222222-2222-2222-2222-222222222222";
    private static final String WARRANTY_ID = "33333333-3333-3333-3333-333333333333";
    private static final OffsetDateTime SHIPMENT_DATE =
            OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String SHIPPING_RATES_JSON_PATH = "$.delivery.shippingRates.id";
    private static final String HANDLING_TIME_JSON_PATH = "$.delivery.handlingTime";
    private static final String DELIVERY_INFO_JSON_PATH = "$.delivery.additionalInfo";
    private static final String SHIPMENT_DATE_JSON_PATH = "$.delivery.shipmentDate";
    private static final String IMPLIED_WARRANTY_JSON_PATH = "$.afterSalesServices.impliedWarranty.id";
    private static final String RETURN_POLICY_JSON_PATH = "$.afterSalesServices.returnPolicy.id";
    private static final String WARRANTY_JSON_PATH = "$.afterSalesServices.warranty.id";

    private static final String DESC_CONTENT = "<h1>Mechanical keyboard</h1>";
    private static final String DESC_IMAGE_URL = "https://img.example/keyboard.jpg";
    private static final String CITY = "Warszawa";
    private static final String DESC_TYPE_JSON_PATH = "$.description.sections[0].items[0].type";
    private static final String DESC_CONTENT_JSON_PATH = "$.description.sections[0].items[0].content";
    private static final String DESC_IMAGE_TYPE_JSON_PATH = "$.description.sections[1].items[0].type";
    private static final String DESC_IMAGE_URL_JSON_PATH = "$.description.sections[1].items[0].url";
    private static final String LOCATION_CITY_JSON_PATH = "$.location.city";
    private static final String ITEM_TYPE_TEXT = "TEXT";
    private static final String ITEM_TYPE_IMAGE = "IMAGE";

    private static final String STARTING_AMOUNT = "1.00";
    private static final String MINIMAL_AMOUNT = "150.00";
    private static final String FORMAT_JSON_PATH = "$.sellingMode.format";
    private static final String STARTING_PRICE_JSON_PATH = "$.sellingMode.startingPrice.amount";
    private static final String MINIMAL_PRICE_JSON_PATH = "$.sellingMode.minimalPrice.amount";
    private static final String STOCK_UNIT_JSON_PATH = "$.stock.unit";
    private static final String FORMAT_AUCTION = "AUCTION";
    private static final String UNIT_PAIR = "PAIR";

    private static final String PARAM_DICT_ID = "11321";
    private static final String PARAM_DICT_VALUE_ID = "1";
    private static final String PARAM_FREE_ID = "11324";
    private static final String PARAM_FREE_VALUE = "Cherry MX";
    private static final String PARAM_FREE_VALUE_JSON_PATH = "$.parameters[0].values[0]";
    private static final String PARAM_RANGE_ID = "12345";
    private static final String PARAM_RANGE_FROM = "10";
    private static final String PARAM_RANGE_TO = "20";

    private static final String EXTERNAL_ID = "SKU-12345";
    private static final String LANGUAGE = "pl-PL";
    private static final String SIZE_TABLE_ID = "size-table-1";
    private static final String EXTERNAL_ID_JSON_PATH = "$.external.id";
    private static final String LANGUAGE_JSON_PATH = "$.language";
    private static final String SIZE_TABLE_ID_JSON_PATH = "$.sizeTable.id";
    private static final String PARAM_ID_JSON_PATH = "$.parameters[0].id";
    private static final String PARAM_DICT_VALUE_JSON_PATH = "$.parameters[0].valuesIds[0]";
    private static final String PARAM_RANGE_ID_JSON_PATH = "$.parameters[1].id";
    private static final String PARAM_RANGE_FROM_JSON_PATH = "$.parameters[1].rangeValue.from";
    private static final String PARAM_RANGE_TO_JSON_PATH = "$.parameters[1].rangeValue.to";

    private static final String PRODUCT_ID = "8f2b1c00-0000-4000-8000-000000000001";
    private static final int PRODUCT_SET_QUANTITY = 2;
    private static final String PRODUCER_ID = "44444444-4444-4444-4444-444444444444";
    private static final String PRODUCT_ID_JSON_PATH = "$.productSet[0].product.id";
    private static final String PRODUCT_QUANTITY_JSON_PATH = "$.productSet[0].quantity.value";
    private static final String PRODUCER_TYPE_JSON_PATH = "$.productSet[0].responsibleProducer.type";
    private static final String PRODUCER_ID_JSON_PATH = "$.productSet[0].responsibleProducer.id";
    private static final String MARKETED_BEFORE_JSON_PATH = "$.productSet[0].marketedBeforeGPSRObligation";
    private static final String PRODUCER_TYPE_ID = "ID";
    private static final String PRODUCT_SET_RESPONSE_BODY = ("{\"id\":\"%s\",\"name\":\"%s\","
            + "\"productSet\":[{\"product\":{\"id\":\"%s\"},\"quantity\":{\"value\":%d},"
            + "\"responsibleProducer\":{\"id\":\"%s\"},\"marketedBeforeGPSRObligation\":true}]}")
            .formatted(CREATED_OFFER_ID, NAME, PRODUCT_ID, PRODUCT_SET_QUANTITY, PRODUCER_ID);

    private static final String BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"INVALID\",\"message\":\"bad name\",\"path\":\"name\"}]}";
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"offer not found\"}]}";

    private static OffersImpl offers(WireMockRuntimeInfo wmInfo) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new org.openapitools.jackson.nullable.JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RetryHandler retryHandler = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        HttpRuntime runtime = new HttpRuntime() {
            @Override public String baseUrl() {
                return wmInfo.getHttpBaseUrl();
            }

            @Override public RetryHandler retryHandler() {
                return retryHandler;
            }

            @Override public AllegroExecutionInterceptor executionInterceptor() {
                return AllegroExecutionInterceptor.noop();
            }

            @Override public ObjectMapper objectMapper() {
                return mapper;
            }

            @Override public Duration readTimeout() {
                return Duration.ofSeconds(5);
            }

            @Override public String requireToken() {
                return TEST_TOKEN;
            }

            @Override public void reauthenticate() {
                // no-op: 401 replay is covered in the transport suite
            }
        };
        return new OffersImpl(runtime);
    }

    private static CreateOfferRequest validRequest() {
        return CreateOfferRequest.builder()
                .name(NAME)
                .categoryId(CATEGORY_ID)
                .buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .build();
    }

    @Test
    void create_whenValidRequest_postsOfferAndReturnsCreated(WireMockRuntimeInfo wmInfo) {
        // given — the create returns the freshly created offer
        stubFor(post(urlEqualTo(CREATE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withHeader("Location", CREATE_LOCATION)
                        .withBodyFile(OFFER_FIXTURE)));

        // when
        Offer created = offers(wmInfo).create(validRequest());

        // then — the request body carries EXACTLY the set fields (strict, no extras):
        // this proves create uses jsonBodyPartial, so the request type's null `language`
        // and pre-initialized empty collections are omitted (the server 400s on `language:null`).
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(equalToJson(("{\"name\":\"%s\",\"category\":{\"id\":\"%s\"},"
                        + "\"sellingMode\":{\"format\":\"BUY_NOW\",\"price\":{\"amount\":\"%s\",\"currency\":\"%s\"}},"
                        + "\"stock\":{\"available\":%d}}").formatted(NAME, CATEGORY_ID, AMOUNT, CURRENCY_PLN, STOCK),
                        true, false)));
        // and the response is mapped to the created offer, with the async operation id parsed
        // from the Location header so the caller can poll operationStatus(id, operationId)
        assertEquals(CREATED_OFFER_ID, created.id());
        assertEquals(CREATE_OPERATION_ID, created.operationId());
    }

    @Test
    void create_whenProductSetProvided_serializesBindingAndReadsItBack(WireMockRuntimeInfo wmInfo) {
        // given — the create echoes a productized offer (product ref + GPSR producer)
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(PRODUCT_SET_RESPONSE_BODY)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME)
                .categoryId(CATEGORY_ID)
                .buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .addProductSetElement(ProductSetElement.of(PRODUCT_ID, PRODUCT_SET_QUANTITY)
                        .withResponsibleProducer(ResponsibleProducerRef.byId(PRODUCER_ID))
                        .withMarketedBeforeGpsrObligation(true))
                .build();

        // when
        Offer created = offers(wmInfo).create(request);

        // then — the product binding + GPSR are on the wire (write mapping)
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(PRODUCT_ID_JSON_PATH, equalTo(PRODUCT_ID)))
                .withRequestBody(matchingJsonPath(PRODUCT_QUANTITY_JSON_PATH,
                        equalTo(String.valueOf(PRODUCT_SET_QUANTITY))))
                .withRequestBody(matchingJsonPath(PRODUCER_TYPE_JSON_PATH, equalTo(PRODUCER_TYPE_ID)))
                .withRequestBody(matchingJsonPath(PRODUCER_ID_JSON_PATH, equalTo(PRODUCER_ID)))
                .withRequestBody(matchingJsonPath(MARKETED_BEFORE_JSON_PATH, equalTo("true"))));
        // and the productSet is read back off the response (read mapping)
        assertEquals(1, created.productSet().size());
        ProductSetElement element = created.productSet().get(0);
        assertEquals(PRODUCT_ID, element.productId());
        assertEquals(PRODUCT_SET_QUANTITY, element.quantity());
        assertEquals(PRODUCER_ID, requireNonNull(element.responsibleProducer()).id());
        assertEquals(Boolean.TRUE, element.marketedBeforeGpsrObligation());
    }

    @Test
    void create_whenImagesProvided_serializesThemInBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK).imageUrls(List.of(IMAGE_URL)).build();

        // when
        offers(wmInfo).create(request);

        // then — the image URL reaches the request body
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(IMAGES_JSON_PATH, equalTo(IMAGE_URL))));
    }

    @Test
    void create_whenDeliveryAndAfterSalesSet_serializesNestedBlocks(WireMockRuntimeInfo wmInfo) {
        // given — a create carrying delivery terms and after-sales conditions
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .delivery(OfferDelivery.builder()
                        .shippingRatesId(SHIPPING_RATES_ID).handlingTime(HANDLING_TIME)
                        .shipmentDate(SHIPMENT_DATE).additionalInfo(DELIVERY_INFO).build())
                .afterSalesServices(AfterSalesServices.builder()
                        .impliedWarrantyId(IMPLIED_WARRANTY_ID).returnPolicyId(RETURN_POLICY_ID)
                        .warrantyId(WARRANTY_ID).build())
                .build();

        // when
        offers(wmInfo).create(request);

        // then — every delivery field and all three after-sales ids reach the body;
        // the after-sales ids are serialized as {"id": "<uuid>"} objects
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(SHIPPING_RATES_JSON_PATH, equalTo(SHIPPING_RATES_ID)))
                .withRequestBody(matchingJsonPath(HANDLING_TIME_JSON_PATH, equalTo(HANDLING_TIME)))
                .withRequestBody(matchingJsonPath(DELIVERY_INFO_JSON_PATH, equalTo(DELIVERY_INFO)))
                .withRequestBody(matchingJsonPath(SHIPMENT_DATE_JSON_PATH))
                .withRequestBody(matchingJsonPath(IMPLIED_WARRANTY_JSON_PATH, equalTo(IMPLIED_WARRANTY_ID)))
                .withRequestBody(matchingJsonPath(RETURN_POLICY_JSON_PATH, equalTo(RETURN_POLICY_ID)))
                .withRequestBody(matchingJsonPath(WARRANTY_JSON_PATH, equalTo(WARRANTY_ID))));
    }

    @Test
    void create_whenDescriptionAndLocationSet_serializesThem(WireMockRuntimeInfo wmInfo) {
        // given — a create carrying a one-section text description and a location
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .description(OfferDescription.of(
                        DescriptionSection.of(DescriptionItem.text(DESC_CONTENT)),
                        DescriptionSection.of(DescriptionItem.image(DESC_IMAGE_URL))))
                .location(OfferLocation.builder().city(CITY).build())
                .build();

        // when
        offers(wmInfo).create(request);

        // then — both item kinds reach the body with their discriminator (TEXT+content,
        // IMAGE+url) and the location does too
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(DESC_TYPE_JSON_PATH, equalTo(ITEM_TYPE_TEXT)))
                .withRequestBody(matchingJsonPath(DESC_CONTENT_JSON_PATH, equalTo(DESC_CONTENT)))
                .withRequestBody(matchingJsonPath(DESC_IMAGE_TYPE_JSON_PATH, equalTo(ITEM_TYPE_IMAGE)))
                .withRequestBody(matchingJsonPath(DESC_IMAGE_URL_JSON_PATH, equalTo(DESC_IMAGE_URL)))
                .withRequestBody(matchingJsonPath(LOCATION_CITY_JSON_PATH, equalTo(CITY))));
    }

    @Test
    void create_whenPureAuction_omitsBuyNowPriceFromBody(WireMockRuntimeInfo wmInfo) {
        // given — a pure auction: a starting price and NO Buy Now price
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).availableStock(STOCK)
                .sellingFormat(OfferFormat.AUCTION)
                .startingPrice(Money.of(STARTING_AMOUNT, CURRENCY_PLN))
                .build();

        // when
        offers(wmInfo).create(request);

        // then — a STRICT body match (no extra elements) proves the selling mode carries
        // the auction starting price and NO `price` (Buy Now) key
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(equalToJson(("{\"name\":\"%s\",\"category\":{\"id\":\"%s\"},"
                        + "\"sellingMode\":{\"format\":\"AUCTION\","
                        + "\"startingPrice\":{\"amount\":\"%s\",\"currency\":\"%s\"}},"
                        + "\"stock\":{\"available\":%d}}")
                        .formatted(NAME, CATEGORY_ID, STARTING_AMOUNT, CURRENCY_PLN, STOCK), true, false)));
    }

    @Test
    void create_whenAuctionSellingTermsSet_serializesFormatPricesAndUnit(WireMockRuntimeInfo wmInfo) {
        // given — an auction with starting/minimal prices and a PAIR stock unit
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .sellingFormat(OfferFormat.AUCTION)
                .startingPrice(Money.of(STARTING_AMOUNT, CURRENCY_PLN))
                .minimalPrice(Money.of(MINIMAL_AMOUNT, CURRENCY_PLN))
                .stockUnit(StockUnit.PAIR)
                .build();

        // when
        offers(wmInfo).create(request);

        // then — the selling format, auction prices, and stock unit reach the body
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(FORMAT_JSON_PATH, equalTo(FORMAT_AUCTION)))
                .withRequestBody(matchingJsonPath(STARTING_PRICE_JSON_PATH, equalTo(STARTING_AMOUNT)))
                .withRequestBody(matchingJsonPath(MINIMAL_PRICE_JSON_PATH, equalTo(MINIMAL_AMOUNT)))
                .withRequestBody(matchingJsonPath(STOCK_UNIT_JSON_PATH, equalTo(UNIT_PAIR))));
    }

    @Test
    void create_whenParametersSet_serializesDictionaryAndRange(WireMockRuntimeInfo wmInfo) {
        // given — a create carrying a dictionary parameter (value ids) and a range parameter
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .addParameter(OfferParameter.dictionary(PARAM_DICT_ID, PARAM_DICT_VALUE_ID))
                .addParameter(OfferParameter.range(PARAM_RANGE_ID, PARAM_RANGE_FROM, PARAM_RANGE_TO))
                .build();

        // when
        offers(wmInfo).create(request);

        // then — both parameters reach the body in order: the dictionary one as
        // {id, valuesIds:[...]} and the range one as {id, rangeValue:{from,to}}
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(PARAM_ID_JSON_PATH, equalTo(PARAM_DICT_ID)))
                .withRequestBody(matchingJsonPath(PARAM_DICT_VALUE_JSON_PATH, equalTo(PARAM_DICT_VALUE_ID)))
                .withRequestBody(matchingJsonPath(PARAM_RANGE_ID_JSON_PATH, equalTo(PARAM_RANGE_ID)))
                .withRequestBody(matchingJsonPath(PARAM_RANGE_FROM_JSON_PATH, equalTo(PARAM_RANGE_FROM)))
                .withRequestBody(matchingJsonPath(PARAM_RANGE_TO_JSON_PATH, equalTo(PARAM_RANGE_TO))));
    }

    @Test
    void create_whenFreeTextParameterSet_serializesValues(WireMockRuntimeInfo wmInfo) {
        // given — a create carrying a single free-text parameter
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .addParameter(OfferParameter.freeText(PARAM_FREE_ID, PARAM_FREE_VALUE))
                .build();

        // when
        offers(wmInfo).create(request);

        // then — the free-text parameter reaches the body as {id, values:[...]}
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(PARAM_ID_JSON_PATH, equalTo(PARAM_FREE_ID)))
                .withRequestBody(matchingJsonPath(PARAM_FREE_VALUE_JSON_PATH, equalTo(PARAM_FREE_VALUE))));
    }

    @Test
    void create_whenOfferRefsSet_serializesExternalLanguageAndSizeTable(WireMockRuntimeInfo wmInfo) {
        // given — a create carrying the seller external id, listing language and a size table
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(OFFER_FIXTURE)));
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN))
                .availableStock(STOCK)
                .externalId(EXTERNAL_ID).language(LANGUAGE).sizeTableId(SIZE_TABLE_ID)
                .build();

        // when
        offers(wmInfo).create(request);

        // then — the external id and size-table id are wrapped as {id:...} objects and the
        // language is a plain scalar
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withRequestBody(matchingJsonPath(EXTERNAL_ID_JSON_PATH, equalTo(EXTERNAL_ID)))
                .withRequestBody(matchingJsonPath(LANGUAGE_JSON_PATH, equalTo(LANGUAGE)))
                .withRequestBody(matchingJsonPath(SIZE_TABLE_ID_JSON_PATH, equalTo(SIZE_TABLE_ID))));
    }

    @Test
    void create_whenRejected_throwsBadRequestWithTypedFieldError(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_BODY)));

        // then
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> offers(wmInfo).create(validRequest()));
        assertEquals("name", failure.errors().get(0).path());
    }

    @Test
    void edit_whenPartialFields_patchesOnlyThoseFields(WireMockRuntimeInfo wmInfo) {
        // given — an edit that changes only the name and stock
        stubFor(patch(urlEqualTo(EDIT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(OFFER_FIXTURE)));
        EditOfferRequest request = EditOfferRequest.builder()
                .name(EDIT_NAME).availableStock(EDIT_STOCK).build();

        // when
        Offer updated = offers(wmInfo).edit(EDIT_OFFER_ID, request);

        // then — the PATCH body carries ONLY the changed fields. A strict match
        // (no extra elements) proves the request type's pre-initialized empty
        // collections (images/parameters/...) and the untouched sellingMode are
        // absent — i.e. jsonBodyPartial omitted null AND empty.
        verify(1, patchRequestedFor(urlEqualTo(EDIT_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(equalToJson(
                        "{\"name\":\"" + EDIT_NAME + "\",\"stock\":{\"available\":" + EDIT_STOCK + "}}",
                        true, false)));
        assertEquals(CREATED_OFFER_ID, updated.id());
    }

    @Test
    void edit_whenPriceAndImages_serializesThoseFields(WireMockRuntimeInfo wmInfo) {
        // given — an edit that changes only the price and images
        stubFor(patch(urlEqualTo(EDIT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(OFFER_FIXTURE)));
        EditOfferRequest request = EditOfferRequest.builder()
                .buyNowPrice(Money.of(AMOUNT, CURRENCY_PLN)).imageUrls(List.of(IMAGE_URL)).build();

        // when
        offers(wmInfo).edit(EDIT_OFFER_ID, request);

        // then — the PATCH carries the selling-mode price and the image, and nothing else
        verify(1, patchRequestedFor(urlEqualTo(EDIT_PATH))
                .withRequestBody(matchingJsonPath(PRICE_JSON_PATH, equalTo(AMOUNT)))
                .withRequestBody(matchingJsonPath(IMAGES_JSON_PATH, equalTo(IMAGE_URL))));
    }

    @Test
    void edit_whenOfferMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(patch(urlEqualTo(EDIT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class,
                () -> offers(wmInfo).edit(EDIT_OFFER_ID, EditOfferRequest.builder().name(EDIT_NAME).build()));
    }

    @Test
    void deleteDraft_whenDraftExists_sendsDelete(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(DELETE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        // when
        offers(wmInfo).deleteDraft(DELETE_OFFER_ID);

        // then — exactly one authenticated DELETE on the draft resource
        verify(1, deleteRequestedFor(urlEqualTo(DELETE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
    }

    @Test
    void deleteDraft_whenOfferMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(DELETE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class, () -> offers(wmInfo).deleteDraft(DELETE_OFFER_ID));
    }
}
