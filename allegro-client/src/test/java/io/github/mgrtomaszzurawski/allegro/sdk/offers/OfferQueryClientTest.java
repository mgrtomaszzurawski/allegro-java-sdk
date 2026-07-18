/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.UnfilledParameters;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OffersImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferQueryClientTest {

    private static final String TEST_TOKEN = "offers-query-token";
    private static final String OFFERS_PATH = "/sale/offers";
    private static final String OFFER_ID = "246810";
    private static final String SMART_PATH = "/sale/offers/" + OFFER_ID + "/smart";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String QUERY_NAME = "name";
    private static final String QUERY_STATUS = "publication.status";
    private static final String QUERY_FORMAT = "sellingMode.format";
    private static final String OFFSET_FIRST = "0";
    private static final String OFFSET_SECOND = "100";
    private static final String PAGE_SIZE = "100";
    private static final int FULL_PAGE = 100;
    private static final int SECOND_PAGE = 30;
    private static final int TOTAL = FULL_PAGE + SECOND_PAGE;

    private static final String FILTER_NAME = "klawiatura";
    private static final String STATUS_ACTIVE_WIRE = "ACTIVE";
    private static final String FORMAT_BUY_NOW_WIRE = "BUY_NOW";
    private static final String OFFER_NAME = "Klawiatura";
    private static final String CATEGORY_ID = "257";
    private static final int AVAILABLE = 7;
    private static final int SOLD = 3;
    private static final String AMOUNT = "199.99";
    private static final String CURRENCY_PLN = "PLN";
    private static final String IMAGE_URL = "https://img.example/x.jpg";

    private static final String RICH_OFFER_PAGE = ("{\"offers\":[{\"id\":\"%s\","
            + "\"name\":\"%s\",\"category\":{\"id\":\"%s\"},"
            + "\"sellingMode\":{\"format\":\"BUY_NOW\",\"price\":{\"amount\":\"%s\",\"currency\":\"%s\"}},"
            + "\"stock\":{\"available\":%d,\"sold\":%d},\"publication\":{\"status\":\"ACTIVE\"},"
            + "\"primaryImage\":{\"url\":\"%s\"}}],\"count\":1}")
            .formatted(OFFER_ID, OFFER_NAME, CATEGORY_ID, AMOUNT, CURRENCY_PLN, AVAILABLE, SOLD, IMAGE_URL);

    private static final String CONDITION_MET_CODE = "DELIVERY";
    private static final String CONDITION_MET_NAME = "Wysyłka";
    private static final String CONDITION_MET_DESCRIPTION = "szybko";
    private static final String UNFILLED_PATH = "/sale/offers/unfilled-parameters";
    private static final String PARAM_ID_ONE = "p-100";
    private static final String PARAM_ID_TWO = "p-200";
    private static final String UNFILLED_ENTRY = "{\"offers\":[{\"id\":\"" + OFFER_ID
            + "\",\"category\":{\"id\":\"" + CATEGORY_ID + "\"},\"parameters\":[{\"id\":\""
            + PARAM_ID_ONE + "\"},{\"id\":\"" + PARAM_ID_TWO + "\"}]}],\"count\":1}";

    private static final String SMART_BODY = "{\"classification\":{\"fulfilled\":true,"
            + "\"lastChanged\":\"2026-01-01T00:00:00Z\"},\"scheduledForReclassification\":false,"
            + "\"conditions\":[{\"code\":\"" + CONDITION_MET_CODE + "\",\"name\":\"" + CONDITION_MET_NAME
            + "\",\"description\":\"" + CONDITION_MET_DESCRIPTION + "\",\"fulfilled\":true},"
            + "{\"code\":\"RETURNS\",\"name\":\"Zwroty\",\"fulfilled\":false}]}";
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"offer not found\"}]}";
    private static final String BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"INVALID\",\"message\":\"bad offer id\",\"path\":\"offerId\"}]}";
    private static final long RETRY_AFTER_SECONDS = 12L;

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

    /** A minimal offers page of {@code size} items (ids only) plus a count field. */
    private static String offerPage(int size) {
        StringBuilder body = new StringBuilder("{\"offers\":[");
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                body.append(',');
            }
            body.append("{\"id\":\"").append(index).append("\"}");
        }
        return body.append("],\"count\":").append(size).append('}').toString();
    }

    @Test
    void streamOffers_whenMultiplePages_streamsAllAndForwardsFilterParams(WireMockRuntimeInfo wmInfo) {
        // given — a full first page (implying more) then a short second page
        stubFor(get(urlPathEqualTo(OFFERS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(offerPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(OFFERS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(offerPage(SECOND_PAGE))));
        OfferFilter filter = OfferFilter.builder()
                .name(FILTER_NAME).status(OfferStatus.ACTIVE).format(OfferFormat.BUY_NOW).build();

        // when — consume the whole stream
        List<OfferSummary> allSummaries = offers(wmInfo).streamOffers(filter).toList();

        // then — every item across both pages, and the filter params survive each page
        assertEquals(TOTAL, allSummaries.size());
        verify(1, getRequestedFor(urlPathEqualTo(OFFERS_PATH))
                .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .withQueryParam(QUERY_LIMIT, equalTo(PAGE_SIZE))
                .withQueryParam(QUERY_NAME, equalTo(FILTER_NAME))
                .withQueryParam(QUERY_STATUS, equalTo(STATUS_ACTIVE_WIRE))
                .withQueryParam(QUERY_FORMAT, equalTo(FORMAT_BUY_NOW_WIRE)));
        verify(1, getRequestedFor(urlPathEqualTo(OFFERS_PATH))
                .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .withQueryParam(QUERY_NAME, equalTo(FILTER_NAME)));
    }

    @Test
    void streamOffers_whenConsumerStopsAtFirstPage_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — two available pages
        stubFor(get(urlPathEqualTo(OFFERS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(offerPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(OFFERS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(offerPage(SECOND_PAGE))));

        // when — take exactly the first page and stop
        long taken = offers(wmInfo).streamOffers(OfferFilter.all()).limit(FULL_PAGE).count();

        // then — laziness: the second page is never requested
        assertEquals(FULL_PAGE, taken);
        verify(1, getRequestedFor(urlPathEqualTo(OFFERS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST)));
        verify(0, getRequestedFor(urlPathEqualTo(OFFERS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
    }

    @Test
    void streamOffers_whenOfferPopulated_mapsEverySummaryField(WireMockRuntimeInfo wmInfo) {
        // given — one fully populated offer
        stubFor(get(urlPathEqualTo(OFFERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(RICH_OFFER_PAGE)));

        // when
        OfferSummary summary = offers(wmInfo).streamOffers(OfferFilter.all()).findFirst().orElseThrow();

        // then — every OfferSummary field, including name
        assertEquals(OFFER_ID, summary.id());
        assertEquals(OFFER_NAME, summary.name());
        assertEquals(CATEGORY_ID, summary.categoryId());
        assertEquals(OfferFormat.BUY_NOW, summary.format());
        assertEquals(OfferStatus.ACTIVE, summary.status());
        assertEquals(Money.of(AMOUNT, CURRENCY_PLN), summary.buyNowPrice());
        assertEquals(AVAILABLE, summary.availableStock());
        assertEquals(SOLD, summary.soldCount());
        assertEquals(IMAGE_URL, summary.primaryImageUrl());
    }

    @Test
    void streamOffers_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(OFFERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        // then — the terminal operation triggers the fetch and surfaces the 5xx
        assertThrows(AllegroServerException.class,
                () -> offers(wmInfo).streamOffers(OfferFilter.all()).findFirst());
    }

    @Test
    void streamOffers_whenRateLimited_reportsRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — 429 with Retry-After; retries are disabled in the harness
        stubFor(get(urlPathEqualTo(OFFERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(RETRY_AFTER_SECONDS))));

        // then
        AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                () -> offers(wmInfo).streamOffers(OfferFilter.all()).findFirst());
        assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
    }

    private static String unfilledPage(int size) {
        StringBuilder body = new StringBuilder("{\"offers\":[");
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                body.append(',');
            }
            body.append("{\"id\":\"").append(index).append("\"}");
        }
        return body.append("],\"count\":").append(size).append('}').toString();
    }

    @Test
    void streamUnfilledParameters_whenEntry_mapsOfferCategoryAndMissingParams(WireMockRuntimeInfo wmInfo) {
        // given — one offer missing two parameters
        stubFor(get(urlPathEqualTo(UNFILLED_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(UNFILLED_ENTRY)));

        // when
        UnfilledParameters entry = offers(wmInfo).streamUnfilledParameters().findFirst().orElseThrow();

        // then
        assertEquals(OFFER_ID, entry.offerId());
        assertEquals(CATEGORY_ID, entry.categoryId());
        assertEquals(List.of(PARAM_ID_ONE, PARAM_ID_TWO), entry.parameterIds());
    }

    @Test
    void streamUnfilledParameters_whenConsumerStopsAtFirstPage_doesNotFetchSecondPage(
            WireMockRuntimeInfo wmInfo) {
        // given — two available pages
        stubFor(get(urlPathEqualTo(UNFILLED_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(unfilledPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(UNFILLED_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(unfilledPage(SECOND_PAGE))));

        // when — take only the first page
        long taken = offers(wmInfo).streamUnfilledParameters().limit(FULL_PAGE).count();

        // then — laziness: the second page is never requested
        assertEquals(FULL_PAGE, taken);
        verify(1, getRequestedFor(urlPathEqualTo(UNFILLED_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST)));
        verify(0, getRequestedFor(urlPathEqualTo(UNFILLED_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
    }

    @Test
    void smartClassification_whenReport_returnsMappedClassification(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SMART_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SMART_BODY)));

        // when
        SmartClassification report = offers(wmInfo).smartClassification(OFFER_ID);

        // then — the qualification flag and the per-condition breakdown map through
        assertTrue(report.fulfilled());
        assertFalse(report.scheduledForReclassification());
        assertEquals(2, report.conditions().size());
        SmartClassification.Condition met = report.conditions().get(0);
        assertEquals(CONDITION_MET_CODE, met.code());
        assertEquals(CONDITION_MET_NAME, met.name());
        assertEquals(CONDITION_MET_DESCRIPTION, met.description());
        assertTrue(met.fulfilled());
        SmartClassification.Condition unmet = report.conditions().get(1);
        assertFalse(unmet.fulfilled());
        // the second condition carries no description — the nullable branch maps to null
        assertNull(unmet.description());
    }

    @Test
    void smartClassification_whenOfferMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SMART_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class, () -> offers(wmInfo).smartClassification(OFFER_ID));
    }

    @Test
    void smartClassification_whenBadRequest_throwsBadRequestWithTypedFieldError(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SMART_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_BODY)));

        // then — the typed field error survives to the caller
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> offers(wmInfo).smartClassification(OFFER_ID));
        assertEquals("offerId", failure.errors().get(0).path());
    }
}
