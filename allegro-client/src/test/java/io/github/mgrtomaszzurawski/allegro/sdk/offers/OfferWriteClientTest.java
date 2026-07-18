/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OffersImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferWriteClientTest {

    private static final String TEST_TOKEN = "offers-write-token";
    private static final String CREATE_PATH = "/sale/product-offers";
    private static final String CREATED_OFFER_ID = "13579";
    private static final String DELETE_OFFER_ID = "97531";
    private static final String DELETE_PATH = "/sale/offers/" + DELETE_OFFER_ID;
    private static final String OFFER_FIXTURE = "offers/product-offer.json";

    private static final String NAME = "Mechanical keyboard";
    private static final String CATEGORY_ID = "257";
    private static final String AMOUNT = "199.99";
    private static final String CURRENCY_PLN = "PLN";
    private static final int STOCK = 10;

    private static final String NAME_JSON_PATH = "$.name";
    private static final String CATEGORY_JSON_PATH = "$.category.id";
    private static final String FORMAT_JSON_PATH = "$.sellingMode.format";
    private static final String PRICE_JSON_PATH = "$.sellingMode.price.amount";
    private static final String STOCK_JSON_PATH = "$.stock.available";
    private static final String IMAGES_JSON_PATH = "$.images[0]";
    private static final String IMAGE_URL = "https://img.example/keyboard.jpg";

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
                        .withBodyFile(OFFER_FIXTURE)));

        // when
        Offer created = offers(wmInfo).create(validRequest());

        // then — the request body pins the essential offer fields
        verify(1, postRequestedFor(urlEqualTo(CREATE_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(NAME_JSON_PATH, equalTo(NAME)))
                .withRequestBody(matchingJsonPath(CATEGORY_JSON_PATH, equalTo(CATEGORY_ID)))
                .withRequestBody(matchingJsonPath(FORMAT_JSON_PATH, equalTo("BUY_NOW")))
                .withRequestBody(matchingJsonPath(PRICE_JSON_PATH, equalTo(AMOUNT)))
                .withRequestBody(matchingJsonPath(STOCK_JSON_PATH, equalTo(String.valueOf(STOCK)))));
        // and the response is mapped to the created offer
        assertEquals(CREATED_OFFER_ID, created.id());
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
