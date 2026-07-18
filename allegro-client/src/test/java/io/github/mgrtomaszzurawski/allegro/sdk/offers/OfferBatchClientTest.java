/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OfferBatchImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferBatchClientTest {

    private static final String TEST_TOKEN = "offers-batch-token";
    private static final String UUID_PATTERN = "[0-9a-fA-F-]{36}";
    private static final String COMMAND_PATH = "/sale/offer-publication-commands/" + UUID_PATTERN;
    private static final String TASKS_PATH = COMMAND_PATH + "/tasks";

    private static final String OFFER_ONE = "111";
    private static final String OFFER_TWO = "222";
    private static final int TASKS_PAGE_SIZE = 100;
    private static final int SECOND_TASKS_PAGE = 30;
    private static final int TOTAL_TASKS = TASKS_PAGE_SIZE + SECOND_TASKS_PAGE;

    private static final String PENDING_REPORT =
            "{\"id\":\"cmd-1\",\"createdAt\":\"2026-01-01T00:00:00Z\","
                    + "\"taskCount\":{\"total\":2,\"success\":0,\"failed\":0}}";
    private static final String COMPLETED_REPORT =
            "{\"id\":\"cmd-1\",\"createdAt\":\"2026-01-01T00:00:00Z\","
                    + "\"completedAt\":\"2026-01-01T00:00:05Z\","
                    + "\"taskCount\":{\"total\":2,\"success\":2,\"failed\":0}}";
    private static final String TWO_TASKS =
            "{\"tasks\":[{\"offer\":{\"id\":\"" + OFFER_ONE + "\"},\"status\":\"SUCCESS\"},"
                    + "{\"offer\":{\"id\":\"" + OFFER_TWO + "\"},\"status\":\"SUCCESS\"}]}";
    private static final String EMPTY_TASKS = "{\"tasks\":[]}";
    private static final String BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"INVALID\",\"message\":\"unknown offer\",\"path\":\"offerCriteria\"}]}";

    private static final String POLL_SCENARIO = "poll";
    private static final String STATE_COMPLETED = "completed";
    private static final String ACTION_JSON_PATH = "$.publication.action";
    private static final String OFFERS_JSON_PATH = "$.offerCriteria[0].offers[0].id";
    private static final String OFFERS_SECOND_JSON_PATH = "$.offerCriteria[0].offers[1].id";
    private static final String TYPE_JSON_PATH = "$.offerCriteria[0].type";

    private static OfferBatchImpl batchClient(WireMockRuntimeInfo wmInfo) {
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
        // A no-op sleeper keeps the poll loop instant; the nanoClock still advances
        // so the timeout budget is real.
        CommandPoller fastPoller = new CommandPoller(
                millis -> { }, System::nanoTime, 1L, 1L, Duration.ofSeconds(30));
        return new OfferBatchImpl(runtime, fastPoller);
    }

    private static String tasksPage(int size) {
        StringBuilder body = new StringBuilder("{\"tasks\":[");
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                body.append(',');
            }
            body.append("{\"offer\":{\"id\":\"").append(index).append("\"},\"status\":\"SUCCESS\"}");
        }
        return body.append("]}").toString();
    }

    private static void stubCompletedCommand() {
        stubFor(put(urlPathMatching(COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
    }

    @Test
    void publish_whenCommandCompletes_submitsActivateAndReturnsReport(WireMockRuntimeInfo wmInfo) {
        // given — a command that is already complete when first polled
        stubCompletedCommand();
        stubFor(get(urlPathMatching(TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));

        // when
        BatchReport report = batchClient(wmInfo).publish(List.of(OFFER_ONE, OFFER_TWO));

        // then — the submitted command asks to ACTIVATE the given offers
        verify(1, putRequestedFor(urlPathMatching(COMMAND_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(ACTION_JSON_PATH, equalTo("ACTIVATE")))
                .withRequestBody(matchingJsonPath(TYPE_JSON_PATH, equalTo("CONTAINS_OFFERS")))
                .withRequestBody(matchingJsonPath(OFFERS_JSON_PATH, equalTo(OFFER_ONE)))
                .withRequestBody(matchingJsonPath(OFFERS_SECOND_JSON_PATH, equalTo(OFFER_TWO))));
        // and the terminal report is mapped
        assertEquals(2, report.total());
        assertEquals(2, report.success());
        assertEquals(0, report.failed());
        assertEquals(2, report.tasks().size());
        assertEquals(OFFER_ONE, report.tasks().get(0).offerId());
    }

    @Test
    void unpublish_whenCommandCompletes_submitsEndAction(WireMockRuntimeInfo wmInfo) {
        // given
        stubCompletedCommand();
        stubFor(get(urlPathMatching(TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));

        // when
        batchClient(wmInfo).unpublish(List.of(OFFER_ONE, OFFER_TWO));

        // then — unpublish maps to the END action
        verify(1, putRequestedFor(urlPathMatching(COMMAND_PATH))
                .withRequestBody(matchingJsonPath(ACTION_JSON_PATH, equalTo("END"))));
    }

    @Test
    void publish_whenCommandPending_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first poll is not complete, the second is
        stubFor(put(urlPathMatching(COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(COMMAND_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PENDING_REPORT))
                .willSetStateTo(STATE_COMPLETED));
        stubFor(get(urlPathMatching(COMMAND_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(STATE_COMPLETED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));

        // when
        BatchReport report = batchClient(wmInfo).publish(List.of(OFFER_ONE, OFFER_TWO));

        // then — the command was polled twice before completing
        assertEquals(2, report.total());
        verify(2, getRequestedFor(urlPathMatching(COMMAND_PATH)));
    }

    @Test
    void publish_whenTasksPaged_gathersEveryPage(WireMockRuntimeInfo wmInfo) {
        // given — the task report spans two pages
        stubCompletedCommand();
        stubFor(get(urlPathMatching(TASKS_PATH)).withQueryParam("offset", equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(tasksPage(TASKS_PAGE_SIZE))));
        stubFor(get(urlPathMatching(TASKS_PATH)).withQueryParam("offset", equalTo("100"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(tasksPage(SECOND_TASKS_PAGE))));

        // when
        BatchReport report = batchClient(wmInfo).publish(List.of(OFFER_ONE));

        // then — every task across both pages is gathered into the report
        assertEquals(TOTAL_TASKS, report.tasks().size());
        verify(1, getRequestedFor(urlPathMatching(TASKS_PATH)).withQueryParam("offset", equalTo("100")));
    }

    @Test
    void publish_whenEmptyTaskPage_stopsGathering(WireMockRuntimeInfo wmInfo) {
        // given — no tasks reported
        stubCompletedCommand();
        stubFor(get(urlPathMatching(TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(EMPTY_TASKS)));

        // when
        BatchReport report = batchClient(wmInfo).publish(List.of(OFFER_ONE));

        // then — an empty first page terminates gathering with no tasks
        assertEquals(0, report.tasks().size());
        verify(1, getRequestedFor(urlPathMatching(TASKS_PATH)));
    }

    @Test
    void publish_whenSubmitRejected_throwsBadRequestWithTypedFieldError(WireMockRuntimeInfo wmInfo) {
        // given — the command submission itself is rejected
        stubFor(put(urlPathMatching(COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_BODY)));

        // then — the typed field error surfaces and no polling happens
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> batchClient(wmInfo).publish(List.of(OFFER_ONE)));
        assertEquals("offerCriteria", failure.errors().get(0).path());
        verify(0, getRequestedFor(urlPathMatching(COMMAND_PATH)));
    }
}
