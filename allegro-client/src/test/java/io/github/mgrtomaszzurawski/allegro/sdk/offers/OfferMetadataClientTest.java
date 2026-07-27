/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferProcessingStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OffersImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferMetadataClientTest {

    private static final String TEST_TOKEN = "metadata-test-token";
    private static final String EVENTS_PATH = "/sale/offer-events";
    private static final String OFFER_ID = "13579";
    private static final String OPERATION_ID = "op-abc";
    private static final String OPERATION_PATH =
            "/sale/product-offers/" + OFFER_ID + "/operations/" + OPERATION_ID;
    private static final int PAGE_SIZE = 100;
    private static final String LIMIT_QUERY = "limit";
    private static final String FROM_QUERY = "from";
    private static final String TYPE_QUERY = "type";

    private static final String EVENTS_SMALL = "{\"offerEvents\":["
            + "{\"id\":\"e1\",\"type\":\"OFFER_PRICE_CHANGED\",\"occurredAt\":\"2026-08-01T10:00:00Z\","
            + "\"offer\":{\"id\":\"o1\"}},"
            + "{\"id\":\"e2\",\"type\":\"OFFER_ACTIVATED\",\"offer\":{\"id\":\"o2\"}}]}";
    private static final String OPERATION_RESPONSE = "{\"offer\":{\"id\":\"" + OFFER_ID + "\"},"
            + "\"operation\":{\"id\":\"" + OPERATION_ID + "\",\"status\":\"COMPLETED\","
            + "\"startedAt\":\"2026-08-01T10:00:00Z\"}}";
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Operation not found\"}]}";

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
                // no-op
            }
        };
        return new OffersImpl(runtime);
    }

    @Test
    void streamEvents_mapsEventsAndExtractsOfferIdPerSubtype(WireMockRuntimeInfo wmInfo) {
        // given — a small page (fewer than PAGE_SIZE) ends the stream
        stubEvents(EVENTS_SMALL, absent());

        // when
        List<OfferEvent> events = offers(wmInfo).streamEvents(OfferEventFilter.all()).toList();

        // then — the polymorphic subtype's offer id is extracted for each event
        assertEquals(2, events.size());
        assertEquals("e1", events.get(0).id());
        assertEquals("OFFER_PRICE_CHANGED", events.get(0).type());
        assertEquals("o1", events.get(0).offerId());
        assertEquals("o2", events.get(1).offerId());
        // the first fetch sends no `from` cursor
        verify(1, getRequestedFor(urlPathEqualTo(EVENTS_PATH)).withQueryParam(FROM_QUERY, absent()));
    }

    @Test
    void streamEvents_isLazy_doesNotFetchPageTwoUntilPageOneConsumed(WireMockRuntimeInfo wmInfo) {
        // given — a full first page (so a next cursor is set), and an empty second page
        String fullPage = "{\"offerEvents\":[" + IntStream.range(0, PAGE_SIZE)
                .mapToObj(index -> "{\"id\":\"e" + index + "\",\"type\":\"OFFER_ACTIVATED\","
                        + "\"offer\":{\"id\":\"o" + index + "\"}}")
                .collect(Collectors.joining(",")) + "]}";
        stubEvents(fullPage, absent());
        stubEvents("{\"offerEvents\":[]}", equalTo("e" + (PAGE_SIZE - 1)));

        // when — consume only the first page
        List<OfferEvent> firstPage = offers(wmInfo).streamEvents(OfferEventFilter.all())
                .limit(PAGE_SIZE).toList();

        // then — page two (from the last event id) is never fetched
        assertEquals(PAGE_SIZE, firstPage.size());
        verify(0, getRequestedFor(urlPathEqualTo(EVENTS_PATH))
                .withQueryParam(FROM_QUERY, equalTo("e" + (PAGE_SIZE - 1))));
    }

    @Test
    void streamEvents_whenTypeFilter_sendsTypeQuery(WireMockRuntimeInfo wmInfo) {
        // given
        stubEvents(EVENTS_SMALL, absent());

        // when
        offers(wmInfo).streamEvents(OfferEventFilter.ofType("OFFER_ENDED")).toList();

        // then
        verify(1, getRequestedFor(urlPathEqualTo(EVENTS_PATH))
                .withQueryParam(TYPE_QUERY, equalTo("OFFER_ENDED")));
    }

    @Test
    void operationStatus_returnsMappedStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(OPERATION_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OPERATION_RESPONSE)));

        // when
        OfferProcessingStatus status = offers(wmInfo).operationStatus(OFFER_ID, OPERATION_ID);

        // then
        verify(1, getRequestedFor(urlEqualTo(OPERATION_PATH)));
        assertEquals(OFFER_ID, status.offerId());
        assertEquals(OPERATION_ID, status.operationId());
        assertEquals("COMPLETED", status.status());
    }

    @Test
    void operationStatus_whenOperationMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given — the operation id is unknown to the server
        stubFor(get(urlEqualTo(OPERATION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_BODY)));

        // when / then
        assertThrows(AllegroNotFoundException.class,
                () -> offers(wmInfo).operationStatus(OFFER_ID, OPERATION_ID));
    }

    @Test
    void operationStatus_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given — the server fails the read
        stubFor(get(urlEqualTo(OPERATION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        // when / then
        assertThrows(AllegroServerException.class,
                () -> offers(wmInfo).operationStatus(OFFER_ID, OPERATION_ID));
    }

    private static void stubEvents(String body, com.github.tomakehurst.wiremock.matching.StringValuePattern from) {
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
                get(urlPathEqualTo(EVENTS_PATH))
                        .withQueryParam(LIMIT_QUERY, equalTo(String.valueOf(PAGE_SIZE)))
                        .withQueryParam(FROM_QUERY, from)
                        .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(body)));
    }
}
