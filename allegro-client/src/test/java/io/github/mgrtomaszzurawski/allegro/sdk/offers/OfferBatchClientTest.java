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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceStockBatchReport;
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
    private static final String PRICE_COMMAND_PATH = "/sale/offer-price-change-commands/" + UUID_PATTERN;
    private static final String PRICE_TASKS_PATH = PRICE_COMMAND_PATH + "/tasks";
    private static final String QTY_COMMAND_PATH = "/sale/offer-quantity-change-commands/" + UUID_PATTERN;
    private static final String QTY_TASKS_PATH = QTY_COMMAND_PATH + "/tasks";

    private static final String NEW_PRICE = "50.00";
    private static final String CURRENCY_PLN = "PLN";
    private static final int NEW_QUANTITY = 25;
    private static final String PRICE_TYPE_JSON_PATH = "$.modification.type";
    private static final String PRICE_AMOUNT_JSON_PATH = "$.modification.price.amount";
    private static final String QTY_CHANGE_TYPE_JSON_PATH = "$.modification.changeType";
    private static final String QTY_VALUE_JSON_PATH = "$.modification.value";

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
    // Provenance: the real error shape live-observed for this endpoint (KNOWN-SERVER-BEHAVIORS
    // §Offers — batch) — a combined element is rejected on modifications[0].
    private static final String BULK_MODIFY_ERROR_PATH = "modifications[0]";
    private static final String BULK_MODIFY_ERROR_CODE = "INVALID_SINGLE_ELEMENT_IN_MODIFICATION";
    private static final String BULK_BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"" + BULK_MODIFY_ERROR_CODE + "\",\"message\":\"Enter exactly "
                    + "one element: 'stock' or 'prices'.\",\"path\":\"" + BULK_MODIFY_ERROR_PATH + "\"}]}";

    private static final String POLL_SCENARIO = "poll";
    private static final String STATE_COMPLETED = "completed";
    private static final String ACTION_JSON_PATH = "$.publication.action";
    private static final String OFFERS_JSON_PATH = "$.offerCriteria[0].offers[0].id";
    private static final String OFFERS_SECOND_JSON_PATH = "$.offerCriteria[0].offers[1].id";
    private static final String TYPE_JSON_PATH = "$.offerCriteria[0].type";

    private static final String BULK_COMMAND_PATH = "/sale/offer-bulk-modification-commands";
    private static final String BULK_STATUS_PATH = BULK_COMMAND_PATH + "/" + UUID_PATTERN;
    private static final String BULK_TASKS_PATH = BULK_STATUS_PATH + "/tasks";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final int STOCK_VALUE = 5;
    private static final String CHANGE_TYPE_FIXED = "FIXED";
    private static final String FIELD_PRICE = "PRICE";
    private static final String FIELD_STOCK = "STOCK";
    private static final String COMMAND_ID_JSON_PATH = "$.commandId";
    // Allegro rejects a modification element carrying both prices and stock
    // (INVALID_SINGLE_ELEMENT_IN_MODIFICATION), so an offer changing both is split
    // into two elements with the same offerId — prices at [0], stock at [1].
    private static final String MOD_OFFER_ID_JSON_PATH = "$.modifications[0].offerId";
    private static final String MOD_SECOND_OFFER_ID_JSON_PATH = "$.modifications[1].offerId";
    private static final String MOD_PRICE_TYPE_JSON_PATH =
            "$.modifications[0].prices['" + MARKETPLACE_PL + "'].changeType";
    private static final String MOD_PRICE_AMOUNT_JSON_PATH =
            "$.modifications[0].prices['" + MARKETPLACE_PL + "'].value.amount";
    private static final String MOD_STOCK_TYPE_JSON_PATH = "$.modifications[1].stock.changeType";
    private static final String MOD_STOCK_VALUE_JSON_PATH = "$.modifications[1].stock.value";
    private static final String SUBJECT_TASKS =
            "{\"tasks\":[{\"subject\":{\"offerId\":\"" + OFFER_ONE + "\",\"field\":\"" + FIELD_PRICE
                    + "\"},\"status\":\"SUCCESS\"},{\"subject\":{\"offerId\":\"" + OFFER_ONE
                    + "\",\"field\":\"" + FIELD_STOCK + "\"},\"status\":\"SUCCESS\"}]}";

    private static final String AUTOMATION_COMMAND_PATH = "/sale/offer-price-automation-commands";
    private static final String AUTOMATION_STATUS_PATH = AUTOMATION_COMMAND_PATH + "/" + UUID_PATTERN;
    private static final String AUTOMATION_TASKS_PATH = AUTOMATION_STATUS_PATH + "/tasks";
    private static final String RULE_ID = "641c73feaef0a8281a3d11f8";
    private static final String MIN_PRICE = "10.00";
    private static final String MAX_PRICE = "500.00";
    private static final String CURRENCY_BASIS_MARKETPLACE = "MARKETPLACE_CURRENCY";
    private static final String ID_JSON_PATH = "$.id";
    private static final String SET_MARKETPLACE_JSON_PATH = "$.modification.set[0].marketplace.id";
    private static final String SET_RULE_JSON_PATH = "$.modification.set[0].rule.id";
    private static final String SET_RANGE_TYPE_JSON_PATH =
            "$.modification.set[0].configuration.priceRange.type";
    private static final String SET_RANGE_MIN_JSON_PATH =
            "$.modification.set[0].configuration.priceRange.minPrice.amount";
    private static final String REMOVE_MARKETPLACE_JSON_PATH = "$.modification.remove[0].marketplace.id";

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

    private static void stubCompletedCommandAt(String commandPath, String tasksPath) {
        stubFor(put(urlPathMatching(commandPath))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(commandPath))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(tasksPath))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));
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
    void changePrices_whenCommandCompletes_submitsFixedPriceToPriceEndpoint(WireMockRuntimeInfo wmInfo) {
        // given
        stubCompletedCommandAt(PRICE_COMMAND_PATH, PRICE_TASKS_PATH);

        // when
        BatchReport report = batchClient(wmInfo)
                .changePrices(List.of(OFFER_ONE), Money.of(NEW_PRICE, CURRENCY_PLN));

        // then — a FIXED_PRICE modification carrying the new amount hits the price endpoint
        verify(1, putRequestedFor(urlPathMatching(PRICE_COMMAND_PATH))
                .withRequestBody(matchingJsonPath(PRICE_TYPE_JSON_PATH, equalTo("FIXED_PRICE")))
                .withRequestBody(matchingJsonPath(PRICE_AMOUNT_JSON_PATH, equalTo(NEW_PRICE)))
                .withRequestBody(matchingJsonPath(OFFERS_JSON_PATH, equalTo(OFFER_ONE))));
        assertEquals(2, report.total());
    }

    @Test
    void changeQuantities_whenCommandCompletes_submitsFixedQuantityToQuantityEndpoint(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubCompletedCommandAt(QTY_COMMAND_PATH, QTY_TASKS_PATH);

        // when
        BatchReport report = batchClient(wmInfo).changeQuantities(List.of(OFFER_ONE), NEW_QUANTITY);

        // then — a FIXED quantity change carrying the new value hits the quantity endpoint
        verify(1, putRequestedFor(urlPathMatching(QTY_COMMAND_PATH))
                .withRequestBody(matchingJsonPath(QTY_CHANGE_TYPE_JSON_PATH, equalTo("FIXED")))
                .withRequestBody(matchingJsonPath(QTY_VALUE_JSON_PATH, equalTo(String.valueOf(NEW_QUANTITY))))
                .withRequestBody(matchingJsonPath(OFFERS_JSON_PATH, equalTo(OFFER_ONE))));
        assertEquals(2, report.total());
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

    @Test
    void modifyPricesAndStock_whenCommandCompletes_postsBetaCommandWithPriceAndStock(
            WireMockRuntimeInfo wmInfo) {
        // given — the POSTed command is already complete when first polled
        stubFor(post(urlPathEqualTo(BULK_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(BULK_STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(BULK_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SUBJECT_TASKS)));

        // when — one offer gets a fixed marketplace price and a fixed stock
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ONE)
                .price(MARKETPLACE_PL, BulkPriceStockModification.PriceChange.fixed(
                        Money.of(NEW_PRICE, CURRENCY_PLN)))
                .stock(BulkPriceStockModification.StockChange.fixed(STOCK_VALUE))
                .build();
        PriceStockBatchReport report = batchClient(wmInfo)
                .modifyPricesAndStock(List.of(modification));

        // then — a beta POST carries the command id and TWO elements for the same
        // offer: a price element and a separate stock element (Allegro rejects a
        // combined element, so the SDK splits them)
        verify(1, postRequestedFor(urlPathEqualTo(BULK_COMMAND_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(matchingJsonPath(COMMAND_ID_JSON_PATH))
                .withRequestBody(matchingJsonPath(MOD_OFFER_ID_JSON_PATH, equalTo(OFFER_ONE)))
                .withRequestBody(matchingJsonPath(MOD_SECOND_OFFER_ID_JSON_PATH, equalTo(OFFER_ONE)))
                .withRequestBody(matchingJsonPath(MOD_PRICE_TYPE_JSON_PATH, equalTo(CHANGE_TYPE_FIXED)))
                .withRequestBody(matchingJsonPath(MOD_PRICE_AMOUNT_JSON_PATH, equalTo(NEW_PRICE)))
                .withRequestBody(matchingJsonPath(MOD_STOCK_TYPE_JSON_PATH, equalTo(CHANGE_TYPE_FIXED)))
                .withRequestBody(matchingJsonPath(MOD_STOCK_VALUE_JSON_PATH,
                        equalTo(String.valueOf(STOCK_VALUE)))));
        // and the terminal report carries the per-field task subjects
        assertEquals(2, report.total());
        assertEquals(2, report.tasks().size());
        assertEquals(OFFER_ONE, report.tasks().get(0).offerId());
        assertEquals(FIELD_PRICE, report.tasks().get(0).field());
        assertEquals(FIELD_STOCK, report.tasks().get(1).field());
    }

    @Test
    void modifyPricesAndStock_whenCommandPending_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first status poll is pending, the second is complete
        stubFor(post(urlPathEqualTo(BULK_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(BULK_STATUS_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PENDING_REPORT))
                .willSetStateTo(STATE_COMPLETED));
        stubFor(get(urlPathMatching(BULK_STATUS_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(STATE_COMPLETED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(BULK_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SUBJECT_TASKS)));

        // when
        PriceStockBatchReport report = batchClient(wmInfo)
                .modifyPricesAndStock(List.of(priceOnlyModification()));

        // then — the command was polled twice before completing
        assertEquals(2, report.total());
        verify(2, getRequestedFor(urlPathMatching(BULK_STATUS_PATH)));
    }

    @Test
    void modifyPricesAndStock_whenTasksPaged_gathersEveryPageWithSubject(WireMockRuntimeInfo wmInfo) {
        // given — the task report spans two pages
        stubFor(post(urlPathEqualTo(BULK_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(BULK_STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(BULK_TASKS_PATH)).withQueryParam("offset", equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(subjectTasksPage(TASKS_PAGE_SIZE))));
        stubFor(get(urlPathMatching(BULK_TASKS_PATH)).withQueryParam("offset", equalTo("100"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(subjectTasksPage(SECOND_TASKS_PAGE))));

        // when
        PriceStockBatchReport report = batchClient(wmInfo)
                .modifyPricesAndStock(List.of(priceOnlyModification()));

        // then — every task across both pages is gathered, subjects mapped
        assertEquals(TOTAL_TASKS, report.tasks().size());
        assertEquals(FIELD_PRICE, report.tasks().get(0).field());
        verify(1, getRequestedFor(urlPathMatching(BULK_TASKS_PATH)).withQueryParam("offset", equalTo("100")));
    }

    @Test
    void modifyPricesAndStock_whenSubmitRejected_throwsBadRequestAndSkipsPolling(
            WireMockRuntimeInfo wmInfo) {
        // given — the command submission itself is rejected
        stubFor(post(urlPathEqualTo(BULK_COMMAND_PATH)).willReturn(
                aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BULK_BAD_REQUEST_BODY)));

        // then — the typed field error surfaces (real path/code) and no polling happens
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> batchClient(wmInfo).modifyPricesAndStock(List.of(priceOnlyModification())));
        assertEquals(BULK_MODIFY_ERROR_PATH, failure.errors().get(0).path());
        assertEquals(BULK_MODIFY_ERROR_CODE, failure.errors().get(0).code());
        verify(0, getRequestedFor(urlPathMatching(BULK_STATUS_PATH)));
    }

    @Test
    void applyPricingRules_whenAssignCompletes_postsSetModificationWithCriteria(
            WireMockRuntimeInfo wmInfo) {
        // given — the POSTed command is already complete when first polled
        stubFor(post(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(AUTOMATION_STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(AUTOMATION_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));

        // when — assign a rule (bounded by a price range) on one marketplace to one offer
        BatchReport report = batchClient(wmInfo).applyPricingRules(assignWithRangeRequest());

        // then — a public.v1 POST carries the command id, the SET branch with the
        // marketplace/rule/price-range, and the offers as a CONTAINS_OFFERS criterion
        verify(1, postRequestedFor(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(ID_JSON_PATH))
                .withRequestBody(matchingJsonPath(SET_MARKETPLACE_JSON_PATH, equalTo(MARKETPLACE_PL)))
                .withRequestBody(matchingJsonPath(SET_RULE_JSON_PATH, equalTo(RULE_ID)))
                .withRequestBody(matchingJsonPath(SET_RANGE_TYPE_JSON_PATH, equalTo(CURRENCY_BASIS_MARKETPLACE)))
                .withRequestBody(matchingJsonPath(SET_RANGE_MIN_JSON_PATH, equalTo(MIN_PRICE)))
                .withRequestBody(matchingJsonPath(TYPE_JSON_PATH, equalTo("CONTAINS_OFFERS")))
                .withRequestBody(matchingJsonPath(OFFERS_JSON_PATH, equalTo(OFFER_ONE))));
        // and the terminal report is mapped
        assertEquals(2, report.total());
        assertEquals(2, report.tasks().size());
        assertEquals(OFFER_ONE, report.tasks().get(0).offerId());
    }

    @Test
    void applyPricingRules_whenAssignWithoutRange_omitsConfiguration(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(AUTOMATION_STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(AUTOMATION_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(EMPTY_TASKS)));

        // when — a config-less assignment
        batchClient(wmInfo).applyPricingRules(BatchPricingRulesRequest.assignRules(List.of(OFFER_ONE))
                .onMarketplace(MARKETPLACE_PL, RULE_ID).build());

        // then — the assignment is submitted...
        verify(1, postRequestedFor(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .withRequestBody(matchingJsonPath(SET_RULE_JSON_PATH, equalTo(RULE_ID))));
        // ...and the optional configuration is OMITTED, not sent as null (partial body):
        // no submitted request carries a configuration node
        verify(0, postRequestedFor(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .withRequestBody(matchingJsonPath("$.modification.set[0].configuration")));
    }

    @Test
    void applyPricingRules_whenRemove_postsRemoveModification(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(AUTOMATION_STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(AUTOMATION_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));

        // when — remove the rules on one marketplace from one offer
        batchClient(wmInfo).applyPricingRules(BatchPricingRulesRequest.removeRules(List.of(OFFER_ONE))
                .fromMarketplace(MARKETPLACE_PL).build());

        // then — the REMOVE branch is submitted with the marketplace id
        verify(1, postRequestedFor(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .withRequestBody(matchingJsonPath(REMOVE_MARKETPLACE_JSON_PATH, equalTo(MARKETPLACE_PL)))
                .withRequestBody(matchingJsonPath(TYPE_JSON_PATH, equalTo("CONTAINS_OFFERS"))));
    }

    @Test
    void applyPricingRules_whenCommandPending_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first status poll is pending, the second is complete
        stubFor(post(urlPathEqualTo(AUTOMATION_COMMAND_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(AUTOMATION_STATUS_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PENDING_REPORT))
                .willSetStateTo(STATE_COMPLETED));
        stubFor(get(urlPathMatching(AUTOMATION_STATUS_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(STATE_COMPLETED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_REPORT)));
        stubFor(get(urlPathMatching(AUTOMATION_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_TASKS)));

        // when
        BatchReport report = batchClient(wmInfo).applyPricingRules(assignWithRangeRequest());

        // then — the command was polled twice before completing
        assertEquals(2, report.total());
        verify(2, getRequestedFor(urlPathMatching(AUTOMATION_STATUS_PATH)));
    }

    @Test
    void applyPricingRules_whenSubmitRejected_throwsBadRequestAndSkipsPolling(
            WireMockRuntimeInfo wmInfo) {
        // given — the command submission itself is rejected
        stubFor(post(urlPathEqualTo(AUTOMATION_COMMAND_PATH)).willReturn(
                aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_BODY)));

        // then — the typed field error surfaces and no polling happens
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> batchClient(wmInfo).applyPricingRules(assignWithRangeRequest()));
        assertEquals("offerCriteria", failure.errors().get(0).path());
        verify(0, getRequestedFor(urlPathMatching(AUTOMATION_STATUS_PATH)));
    }

    private static BatchPricingRulesRequest assignWithRangeRequest() {
        return BatchPricingRulesRequest.assignRules(List.of(OFFER_ONE))
                .onMarketplace(MARKETPLACE_PL, RULE_ID, BatchPricingRulesRequest.PriceRange.of(
                        BatchPricingRulesRequest.PriceRange.CurrencyBasis.MARKETPLACE_CURRENCY,
                        Money.of(MIN_PRICE, CURRENCY_PLN), Money.of(MAX_PRICE, CURRENCY_PLN)))
                .build();
    }

    private static BulkPriceStockModification priceOnlyModification() {
        return BulkPriceStockModification.forOffer(OFFER_ONE)
                .price(MARKETPLACE_PL, BulkPriceStockModification.PriceChange.gain(
                        Money.of(NEW_PRICE, CURRENCY_PLN)))
                .build();
    }

    private static String subjectTasksPage(int size) {
        StringBuilder body = new StringBuilder("{\"tasks\":[");
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                body.append(',');
            }
            body.append("{\"subject\":{\"offerId\":\"").append(index).append("\",\"field\":\"")
                    .append(FIELD_PRICE).append("\"},\"status\":\"SUCCESS\"}");
        }
        return body.append("]}").toString();
    }
}
