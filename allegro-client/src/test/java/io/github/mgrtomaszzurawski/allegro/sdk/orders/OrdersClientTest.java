/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.PointsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.SerialNumbersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Carrier;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.CarrierTracking;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEventStats;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEventType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.PaymentProvider;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.PaymentType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.PickupPoint;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Waybill;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the orders facade: the flagship {@code get(id)}
 * mapping and mandatory error-path table (400 typed / 401 replay / 404 / 429 /
 * 5xx), plus the order-management surface — lazy offset/cursor streaming, the
 * status/serial/tracking writes (verified on the wire), and the dictionary reads.
 */
@WireMockTest
class OrdersClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RETRY = "retry-5xx";
    private static final String STATE_RECOVERED = "recovered";

    // Order fixture values — kept in sync with __files/orders/order.json so the
    // wire bytes and the expected mapping share one source of truth.
    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final String ORDER_BODY_FILE = "orders/order.json";
    private static final String CHECKOUT_FORMS_PATH = "/order/checkout-forms";
    private static final String ORDER_PATH = CHECKOUT_FORMS_PATH + "/" + ORDER_ID;
    private static final String EXPECTED_BUYER_LOGIN = "test-buyer";
    private static final String EXPECTED_BUYER_EMAIL = "buyer@example.com";
    private static final String EXPECTED_OFFER_ID = "12345";
    private static final String EXPECTED_OFFER_NAME = "Test Widget";
    private static final int EXPECTED_QUANTITY = 2;
    private static final String EXPECTED_UNIT_AMOUNT = "19.99";
    private static final String EXPECTED_TOTAL_AMOUNT = "39.98";
    private static final String EXPECTED_CURRENCY = "PLN";
    private static final String EXPECTED_MARKETPLACE_ID = "allegro-pl";
    private static final String EXPECTED_REVISION = "abc123";
    private static final String EXPECTED_MESSAGE = "Please ship fast";
    private static final String EXPECTED_PAID_AMOUNT = "44.98";
    private static final String EXPECTED_SURCHARGE_AMOUNT = "5.00";
    private static final String EXPECTED_SELLER_NOTE = "Fragile - handle with care";
    private static final String FUTURE_PAYMENT_TYPE = "SOME_FUTURE_PAYMENT_TYPE";
    private static final String FUTURE_PAYMENT_PROVIDER = "SOME_FUTURE_PROVIDER";

    private static final long RETRY_AFTER_SECONDS = 120L;
    private static final int FAST_MAX_ATTEMPTS = 2;
    // Original request + one transparent replay/retry = two wire requests.
    private static final int EXPECTED_REQUESTS_WITH_ONE_REPLAY = 2;
    private static final int EXPECTED_REQUESTS_WITH_ONE_RETRY = 2;

    // Order-management paths and query-parameter names under test.
    private static final String EVENTS_PATH = "/order/events";
    private static final String EVENT_STATS_PATH = "/order/event-stats";
    private static final String CARRIERS_PATH = "/order/carriers";
    private static final String PICKUP_POINTS_PATH = "/order/carriers/ALLEGRO/points";
    private static final String FULFILLMENT_PATH = ORDER_PATH + "/fulfillment";
    private static final String SERIAL_NUMBERS_PATH = ORDER_PATH + "/serial-numbers";
    private static final String SHIPMENTS_PATH = ORDER_PATH + "/shipments";
    private static final String BILLING_LINKS_PATH = "/order/" + ORDER_ID + "/billing-documents/links";
    private static final String CARRIER_TRACKING_PATH = "/order/carriers/DPD/tracking";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_FROM = "from";
    private static final String PARAM_WAYBILL = "waybill";
    private static final String PARAM_CARRIERS = "carriers";
    private static final String PARAM_REVISION = "checkoutForm.revision";
    private static final String PARAM_BUYER_LOGIN = "buyer.login";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_FULFILLMENT_STATUS = "fulfillment.status";
    private static final String PARAM_TYPE = "type";
    private static final String STATUS_READY_FOR_PROCESSING = "READY_FOR_PROCESSING";
    // Wire values no current SDK release models — used to prove forward-compat:
    // an unknown enum value degrades to the UNKNOWN sentinel instead of throwing,
    // and the UNKNOWN sentinel is never sent back as a filter query parameter.
    private static final String FUTURE_STATUS = "SOME_FUTURE_STATUS";
    private static final String FUTURE_EVENT_TYPE = "SOME_FUTURE_EVENT_TYPE";
    private static final String JSONPATH_STATUS = "$.status";
    private static final String JSONPATH_LINE_ITEM_ID = "$.lineItems[0].id";
    private static final String JSONPATH_SERIAL_VALUE = "$.lineItems[0].serialNumbers.entries[0].value";
    private static final String JSONPATH_CARRIER_ID = "$.carrierId";
    private static final String JSONPATH_WAYBILL = "$.waybill";
    private static final String JSONPATH_URL = "$.url";
    private static final String EVENT_TIME = "2026-01-01T00:00:00Z";
    private static final String EVENT_ORDER_ID_PREFIX = "o-";
    private static final String EVENT_ORDER_REVISION = "r1";
    private static final String POINT_CITY = "Warsaw";
    private static final String BLANK_REVISION = "  ";

    // Full first page + short second page prove lazy offset pagination.
    private static final int FULL_PAGE = 100;
    private static final int SECOND_PAGE = 50;
    private static final int TOTAL_ORDERS = FULL_PAGE + SECOND_PAGE;
    private static final String OFFSET_PAGE_TWO = String.valueOf(FULL_PAGE);

    private static final String CARRIER_ID = "DPD";
    private static final String CARRIER_NAME = "DPD";
    private static final String WAYBILL_ONE = "WB-1";
    private static final String CREATED_WAYBILL_ID = "way-2";
    private static final String CREATED_WAYBILL = "WB-2";
    private static final String LINE_ITEM_UUID = "0f3e2b1a-1111-2222-3333-444455556666";
    private static final String SERIAL_ONE = "SN-1";
    private static final String DOC_URL = "https://docs.example.com/invoice-1.pdf";
    private static final String EVENT_ID_ONE = "evt-1";
    private static final String EVENT_ID_TWO = "evt-2";
    private static final String EVENT_ID_THREE = "evt-3";
    private static final String LATEST_EVENT_ID = "evt-9";
    private static final String TRACKING_CODE = "DELIVERED";
    private static final String POINT_ID = "point-1";
    private static final String POINT_CARRIER = "UPS";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified. A sandbox order (buyer-seeded) will
    // confirm or correct the errors[] shape during the bucket's exploration pass.
    private static final String BAD_REQUEST_BODY = """
            {"errors":[{"code":"InvalidRevision","message":"Revision is stale",
              "userMessage":"Zamowienie zostalo zmienione","path":"checkoutForm.revision",
              "details":"expected newer revision"}]}
            """;
    private static final String NOT_FOUND_BODY = """
            {"errors":[{"code":"OrderNotFound","message":"Order not found",
              "userMessage":"Nie znaleziono zamowienia","path":null}]}
            """;
    private static final String RATE_LIMIT_BODY = """
            {"errors":[{"code":"TooManyRequests","message":"Slow down"}]}
            """;
    // spec-derived: not yet wire-verified. Confirms null-safety of optional
    // fields (no fulfillment, no marketplace, no optional buyer fields, empty
    // line items).
    private static final String LEAN_ORDER_BODY = """
            {"id":"a8f6c3e2-1111-2222-3333-444455556666","status":"BOUGHT",
             "buyer":{"id":"44556677","email":"buyer@example.com","login":"test-buyer"},
             "lineItems":[],
             "summary":{"totalToPay":{"amount":"0.00","currency":"PLN"}}}
            """;
    // spec-derived: forward-compat probe. A wire status/fulfillment.status the SDK
    // does not model must degrade to the UNKNOWN sentinel, not fail the response.
    // Built by concatenation (not a text block) so the FUTURE_STATUS constant is
    // the single source of the unmodelled wire value.
    private static final String UNKNOWN_ENUM_ORDER_BODY =
            "{\"id\":\"a8f6c3e2-1111-2222-3333-444455556666\",\"status\":\"" + FUTURE_STATUS + "\","
            + "\"buyer\":{\"id\":\"44556677\",\"email\":\"buyer@example.com\",\"login\":\"test-buyer\"},"
            + "\"lineItems\":[],"
            + "\"fulfillment\":{\"status\":\"" + FUTURE_STATUS + "\"},"
            + "\"summary\":{\"totalToPay\":{\"amount\":\"0.00\",\"currency\":\"PLN\"}}}";

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return client(wmInfo, RetryPolicy.defaults());
    }

    private static AllegroClient client(WireMockRuntimeInfo wmInfo, RetryPolicy retryPolicy) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(retryPolicy)
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    // spec-derived: minimal but valid checkout-form shape the mapper needs.
    private static String orderJson(int index) {
        String orderUuid = String.format("00000000-0000-0000-0000-%012d", index);
        return "{\"id\":\"" + orderUuid + "\",\"status\":\"" + STATUS_READY_FOR_PROCESSING + "\","
                + "\"buyer\":{\"id\":\"1\",\"login\":\"b\",\"email\":\"b@example.com\"},"
                + "\"lineItems\":[],"
                + "\"summary\":{\"totalToPay\":{\"amount\":\"10.00\",\"currency\":\"PLN\"}}}";
    }

    private static String ordersPage(int count, int startIndex) {
        StringBuilder forms = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                forms.append(',');
            }
            forms.append(orderJson(startIndex + index));
        }
        return "{\"checkoutForms\":[" + forms + "],\"count\":" + count
                + ",\"totalCount\":" + TOTAL_ORDERS + "}";
    }

    private static String eventJson(String eventId) {
        return "{\"id\":\"" + eventId + "\",\"type\":\"" + OrderEventType.BOUGHT.name() + "\","
                + "\"occurredAt\":\"" + EVENT_TIME + "\","
                + "\"order\":{\"checkoutForm\":{\"id\":\"" + EVENT_ORDER_ID_PREFIX + eventId
                + "\",\"revision\":\"" + EVENT_ORDER_REVISION + "\"}}}";
    }

    private static String eventsBody(String... eventIds) {
        StringBuilder events = new StringBuilder();
        for (int index = 0; index < eventIds.length; index++) {
            if (index > 0) {
                events.append(',');
            }
            events.append(eventJson(eventIds[index]));
        }
        return "{\"events\":[" + events + "]}";
    }

    @Test
    void get_whenOrderExists_mapsFlagshipRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(ORDER_BODY_FILE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Order order = allegro.orders().get(ORDER_ID);

            // then — Raw graph mapped to the immutable domain record
            assertEquals(ORDER_ID, order.id());
            assertEquals(OrderStatus.READY_FOR_PROCESSING, order.status());
            assertEquals(SellerStatus.NEW, order.sellerStatus());
            assertEquals(EXPECTED_MESSAGE, order.messageToSeller());
            assertEquals(EXPECTED_MARKETPLACE_ID, order.marketplaceId());
            assertEquals(EXPECTED_REVISION, order.revision());

            assertEquals(EXPECTED_BUYER_LOGIN, order.buyer().login());
            assertEquals(EXPECTED_BUYER_EMAIL, order.buyer().email());
            assertFalse(order.buyer().guest());

            assertEquals(1, order.lineItems().size());
            var lineItem = order.lineItems().get(0);
            assertEquals(EXPECTED_OFFER_ID, lineItem.offerId());
            assertEquals(EXPECTED_OFFER_NAME, lineItem.offerName());
            assertEquals(EXPECTED_QUANTITY, lineItem.quantity());
            assertEquals(EXPECTED_UNIT_AMOUNT, lineItem.price().amount());
            assertEquals(EXPECTED_CURRENCY, lineItem.price().currency());

            assertEquals(EXPECTED_TOTAL_AMOUNT, order.totalToPay().amount());
            assertEquals(EXPECTED_CURRENCY, order.totalToPay().currency());

            // payment breakdown, surcharges, and the seller's private note
            assertEquals(PaymentType.ONLINE, order.payment().type());
            assertEquals(PaymentProvider.PAYU, order.payment().provider());
            assertEquals(EXPECTED_PAID_AMOUNT, order.payment().paidAmount().amount());
            assertNotNull(order.payment().finishedAt());
            assertEquals(1, order.surcharges().size());
            assertEquals(EXPECTED_SURCHARGE_AMOUNT, order.surcharges().get(0).paidAmount().amount());
            assertEquals(EXPECTED_SELLER_NOTE, order.sellerNote());
            verify(1, getRequestedFor(urlEqualTo(ORDER_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — first acquisition hands out token-one, second hands out token-two
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(ORDER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(ORDER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(ORDER_BODY_FILE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Order order = allegro.orders().get(ORDER_ID);

            // then — replayed exactly once, the replay carried the FRESH token
            assertEquals(ORDER_ID, order.id());
            verify(EXPECTED_REQUESTS_WITH_ONE_REPLAY, getRequestedFor(urlEqualTo(ORDER_PATH)));
            verify(1, getRequestedFor(urlEqualTo(ORDER_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when400WithErrors_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var orders = allegro.orders();

            // then — the errors[] payload survives as typed field errors
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> orders.get(ORDER_ID));
            assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals(1, failure.errors().size());
            AllegroFieldError fieldError = failure.errors().get(0);
            assertEquals("InvalidRevision", fieldError.code());
            assertEquals("checkoutForm.revision", fieldError.path());
        }
    }

    @Test
    void get_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var orders = allegro.orders();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, () -> orders.get(ORDER_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertTrue(failure.responseBody().contains("OrderNotFound"));
        }
    }

    @Test
    void get_when429Exhausted_throwsRateLimitWithRetryAfterSeconds(WireMockRuntimeInfo wmInfo) {
        // given — always 429; a fast policy (no Retry-After sleep) exhausts quickly
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                Long.toString(RETRY_AFTER_SECONDS))
                        .withBody(RATE_LIMIT_BODY)));
        RetryPolicy fast = RetryPolicy.builder()
                .maxAttempts(FAST_MAX_ATTEMPTS)
                .maxRetryAfterSeconds(0L)
                .build();

        try (AllegroClient allegro = client(wmInfo, fast)) {
            var orders = allegro.orders();

            // then — retried to exhaustion, then the typed rate-limit failure
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, () -> orders.get(ORDER_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(ORDER_PATH)));
        }
    }

    @Test
    void get_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — GETs are idempotent, so a transient 500 is retried
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(ORDER_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(ORDER_BODY_FILE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Order order = allegro.orders().get(ORDER_ID);

            // then — the retry recovered the call
            assertEquals(ORDER_ID, order.id());
            verify(EXPECTED_REQUESTS_WITH_ONE_RETRY, getRequestedFor(urlEqualTo(ORDER_PATH)));
        }
    }

    @Test
    void orders_whenClientClosed_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given
        AllegroClient allegro = client(wmInfo);
        allegro.close();

        // then
        assertThrows(IllegalStateException.class, allegro::orders);
    }

    @Test
    void get_whenOptionalFieldsAbsent_leavesThemNull(WireMockRuntimeInfo wmInfo) {
        // given — a leaner order: no fulfillment, marketplace, message, or
        // optional buyer fields, and no line items
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(LEAN_ORDER_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Order order = allegro.orders().get(ORDER_ID);

            // then — optional order fields map to null / empty, not to a crash
            assertNull(order.sellerStatus());
            assertNull(order.marketplaceId());
            assertNull(order.messageToSeller());
            assertNull(order.payment());
            assertNull(order.sellerNote());
            assertTrue(order.lineItems().isEmpty());
            assertTrue(order.surcharges().isEmpty());
            // and optional buyer fields map to null, guest defaults to false
            assertNull(order.buyer().firstName());
            assertNull(order.buyer().lastName());
            assertNull(order.buyer().companyName());
            assertNull(order.buyer().phoneNumber());
            assertFalse(order.buyer().guest());
        }
    }

    @Test
    void streamOrders_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more (totalCount > page)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHECKOUT_FORMS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ordersPage(FULL_PAGE, 0))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — consume only the first order
            List<Order> firstOnly = allegro.orders().streamOrders(OrderFilter.all())
                    .limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo("0")));
            verify(0, getRequestedFor(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamOrders_whenTraversed_fetchesAllPagesAndTerminates(WireMockRuntimeInfo wmInfo) {
        // given — full page then a short page (offset+count reaches totalCount)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHECKOUT_FORMS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ordersPage(FULL_PAGE, 0))));
        stubFor(get(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ordersPage(SECOND_PAGE, FULL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.orders().streamOrders(OrderFilter.all()).count();

            // then — both pages walked exactly once, stream terminated
            assertEquals(TOTAL_ORDERS, total);
            verify(1, getRequestedFor(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo("0")));
            verify(1, getRequestedFor(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamOrders_whenFilterGiven_carriesFilterAcrossPageBoundary(WireMockRuntimeInfo wmInfo) {
        // given — both pages are stubbed to require the filter query params, so a
        // request that dropped them on page two would miss the stub and 404
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo("0"))
                .withQueryParam(PARAM_BUYER_LOGIN, equalTo(EXPECTED_BUYER_LOGIN))
                .withQueryParam(PARAM_STATUS, equalTo(STATUS_READY_FOR_PROCESSING))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ordersPage(FULL_PAGE, 0))));
        stubFor(get(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                .withQueryParam(PARAM_BUYER_LOGIN, equalTo(EXPECTED_BUYER_LOGIN))
                .withQueryParam(PARAM_STATUS, equalTo(STATUS_READY_FOR_PROCESSING))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ordersPage(SECOND_PAGE, FULL_PAGE))));
        OrderFilter filter = OrderFilter.builder()
                .buyerLogin(EXPECTED_BUYER_LOGIN)
                .statuses(OrderStatus.READY_FOR_PROCESSING)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.orders().streamOrders(filter).count();

            // then — page two carried the filter params (else it would not match)
            assertEquals(TOTAL_ORDERS, total);
            verify(1, getRequestedFor(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                    .withQueryParam(PARAM_BUYER_LOGIN, equalTo(EXPECTED_BUYER_LOGIN)));
        }
    }

    @Test
    void streamEvents_whenConsumingFirstElement_doesNotFetchNextCursorPage(WireMockRuntimeInfo wmInfo) {
        // given — first page (no cursor) carries two events
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(EVENTS_PATH)).withQueryParam(PARAM_FROM, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(eventsBody(EVENT_ID_ONE, EVENT_ID_TWO))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — consume only the first event
            List<OrderEvent> firstOnly =
                    allegro.orders().streamEvents(OrderEventFilter.all()).limit(1).toList();

            // then — the event's fields are mapped from the Raw graph
            assertEquals(1, firstOnly.size());
            OrderEvent event = firstOnly.get(0);
            assertEquals(EVENT_ID_ONE, event.id());
            assertEquals(OrderEventType.BOUGHT, event.type());
            assertEquals(OffsetDateTime.parse(EVENT_TIME), event.occurredAt());
            assertEquals(EVENT_ORDER_ID_PREFIX + EVENT_ID_ONE, event.orderId());
            assertEquals(EVENT_ORDER_REVISION, event.orderRevision());
            // and the next page (from=last id) is never fetched
            verify(1, getRequestedFor(urlPathEqualTo(EVENTS_PATH)).withQueryParam(PARAM_FROM, absent()));
            verify(0, getRequestedFor(urlPathEqualTo(EVENTS_PATH))
                    .withQueryParam(PARAM_FROM, equalTo(EVENT_ID_TWO)));
        }
    }

    @Test
    void streamEvents_whenTraversed_advancesCursorByLastEventId(WireMockRuntimeInfo wmInfo) {
        // given — cursor walk: [e1,e2] -> from=e2 [e3] -> from=e3 [] (stop)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(EVENTS_PATH)).withQueryParam(PARAM_FROM, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(eventsBody(EVENT_ID_ONE, EVENT_ID_TWO))));
        stubFor(get(urlPathEqualTo(EVENTS_PATH)).withQueryParam(PARAM_FROM, equalTo(EVENT_ID_TWO))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(eventsBody(EVENT_ID_THREE))));
        stubFor(get(urlPathEqualTo(EVENTS_PATH)).withQueryParam(PARAM_FROM, equalTo(EVENT_ID_THREE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(eventsBody())));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.orders().streamEvents(OrderEventFilter.all()).count();

            // then — all three events read, cursor advanced by the last id each step
            assertEquals(3, total);
            verify(1, getRequestedFor(urlPathEqualTo(EVENTS_PATH))
                    .withQueryParam(PARAM_FROM, equalTo(EVENT_ID_TWO)));
            verify(1, getRequestedFor(urlPathEqualTo(EVENTS_PATH))
                    .withQueryParam(PARAM_FROM, equalTo(EVENT_ID_THREE)));
        }
    }

    @Test
    void eventStats_whenCalled_mapsLatestEventMarker(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(EVENT_STATS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"latestEvent\":{\"id\":\"" + LATEST_EVENT_ID
                                + "\",\"occurredAt\":\"" + EVENT_TIME + "\"}}")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OrderEventStats stats = allegro.orders().eventStats();

            // then
            assertEquals(LATEST_EVENT_ID, stats.latestEventId());
            assertEquals(OffsetDateTime.parse(EVENT_TIME), stats.latestEventOccurredAt());
        }
    }

    @Test
    void markStatus_whenNoRevision_putsStatusWithoutRevisionQuery(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlPathEqualTo(FULFILLMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().markStatus(ORDER_ID, SellerStatus.SENT);

            // then — status in the body, no optimistic-concurrency query param
            verify(1, putRequestedFor(urlPathEqualTo(FULFILLMENT_PATH))
                    .withQueryParam(PARAM_REVISION, absent())
                    .withRequestBody(matchingJsonPath(JSONPATH_STATUS,
                            equalTo(SellerStatus.SENT.name()))));
        }
    }

    @Test
    void markStatus_whenRevisionBlank_throwsIllegalArgumentBeforeWrite(WireMockRuntimeInfo wmInfo) {
        // given — no fulfillment stub: the guard must reject before any HTTP call
        try (AllegroClient allegro = client(wmInfo)) {
            var orders = allegro.orders();

            // then — a blank revision fails fast, never silently degrading to last-write-wins
            assertThrows(IllegalArgumentException.class,
                    () -> orders.markStatus(ORDER_ID, SellerStatus.SENT, BLANK_REVISION));
            verify(0, putRequestedFor(urlPathEqualTo(FULFILLMENT_PATH)));
        }
    }

    @Test
    void markStatus_whenRevisionGiven_putsStatusWithRevisionQuery(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlPathEqualTo(FULFILLMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().markStatus(ORDER_ID, SellerStatus.PROCESSING, EXPECTED_REVISION);

            // then — the revision travels as checkoutForm.revision
            verify(1, putRequestedFor(urlPathEqualTo(FULFILLMENT_PATH))
                    .withQueryParam(PARAM_REVISION, equalTo(EXPECTED_REVISION))
                    .withRequestBody(matchingJsonPath(JSONPATH_STATUS,
                            equalTo(SellerStatus.PROCESSING.name()))));
        }
    }

    @Test
    void setSerialNumbers_whenCalled_postsNestedSerialNumbersBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlPathEqualTo(SERIAL_NUMBERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        SerialNumbersRequest request = SerialNumbersRequest.builder()
                .lineItem(LINE_ITEM_UUID, SERIAL_ONE)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().setSerialNumbers(ORDER_ID, request);

            // then — the nested request body reaches the wire
            verify(1, postRequestedFor(urlPathEqualTo(SERIAL_NUMBERS_PATH))
                    .withRequestBody(matchingJsonPath(JSONPATH_LINE_ITEM_ID, equalTo(LINE_ITEM_UUID)))
                    .withRequestBody(matchingJsonPath(JSONPATH_SERIAL_VALUE, equalTo(SERIAL_ONE))));
        }
    }

    @Test
    void setSerialNumbers_whenRevisionBlank_throwsIllegalArgumentBeforeWrite(WireMockRuntimeInfo wmInfo) {
        // given
        SerialNumbersRequest request = SerialNumbersRequest.builder()
                .lineItem(LINE_ITEM_UUID, SERIAL_ONE)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            var orders = allegro.orders();

            // then — a blank revision is rejected before any HTTP call
            assertThrows(IllegalArgumentException.class,
                    () -> orders.setSerialNumbers(ORDER_ID, request, BLANK_REVISION));
            verify(0, postRequestedFor(urlPathEqualTo(SERIAL_NUMBERS_PATH)));
        }
    }

    @Test
    void attachBillingDocumentLink_whenCalled_postsUrl(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlPathEqualTo(BILLING_LINKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().attachBillingDocumentLink(ORDER_ID, DOC_URL);

            // then
            verify(1, postRequestedFor(urlPathEqualTo(BILLING_LINKS_PATH))
                    .withRequestBody(matchingJsonPath(JSONPATH_URL, equalTo(DOC_URL))));
        }
    }

    @Test
    void trackingNumbers_whenCalled_mapsWaybills(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SHIPMENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"shipments\":[{\"id\":\"way-1\",\"waybill\":\"" + WAYBILL_ONE
                                + "\",\"carrierId\":\"" + CARRIER_ID + "\",\"carrierName\":\""
                                + CARRIER_NAME + "\",\"lineItems\":[{\"id\":\"li-1\"}],"
                                + "\"createdAt\":\"2026-01-01T00:00:00Z\"}]}")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Waybill> waybills = allegro.orders().trackingNumbers(ORDER_ID);

            // then
            assertEquals(1, waybills.size());
            assertEquals(WAYBILL_ONE, waybills.get(0).waybill());
            assertEquals(CARRIER_ID, waybills.get(0).carrierId());
            assertEquals(List.of("li-1"), waybills.get(0).lineItemIds());
        }
    }

    @Test
    void addTrackingNumber_whenCalled_postsShipmentAndReturnsWaybill(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlPathEqualTo(SHIPMENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody("{\"id\":\"" + CREATED_WAYBILL_ID + "\",\"waybill\":\""
                                + CREATED_WAYBILL + "\",\"carrierId\":\"" + CARRIER_ID
                                + "\",\"lineItems\":[],\"createdAt\":\"2026-01-01T00:00:00Z\"}")));
        ShipmentRequest request = ShipmentRequest.builder()
                .carrierId(CARRIER_ID)
                .waybill(CREATED_WAYBILL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Waybill created = allegro.orders().addTrackingNumber(ORDER_ID, request);

            // then — response mapped, and the request carried the shipment fields
            assertEquals(CREATED_WAYBILL_ID, created.id());
            assertEquals(CREATED_WAYBILL, created.waybill());
            verify(1, postRequestedFor(urlPathEqualTo(SHIPMENTS_PATH))
                    .withRequestBody(matchingJsonPath(JSONPATH_CARRIER_ID, equalTo(CARRIER_ID)))
                    .withRequestBody(matchingJsonPath(JSONPATH_WAYBILL, equalTo(CREATED_WAYBILL))));
        }
    }

    @Test
    void addTrackingNumber_when5xx_doesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given — a POST is not retried by default, even on a transient 500
        stubToken(TEST_TOKEN);
        stubFor(post(urlPathEqualTo(SHIPMENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        ShipmentRequest request = ShipmentRequest.builder()
                .carrierId(CARRIER_ID)
                .waybill(CREATED_WAYBILL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            var orders = allegro.orders();

            // then — one attempt only
            assertThrows(AllegroServerException.class,
                    () -> orders.addTrackingNumber(ORDER_ID, request));
            verify(1, postRequestedFor(urlPathEqualTo(SHIPMENTS_PATH)));
        }
    }

    @Test
    void carriers_whenCalled_mapsCarrierDictionary(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CARRIERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"carriers\":[{\"id\":\"DPD\",\"name\":\"DPD\"},"
                                + "{\"id\":\"UPS\",\"name\":\"UPS\"}]}")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Carrier> carriers = allegro.orders().carriers();

            // then
            assertEquals(2, carriers.size());
            assertEquals(CARRIER_ID, carriers.get(0).id());
        }
    }

    @Test
    void carrierTracking_whenCalled_sendsWaybillQueryAndMapsHistory(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CARRIER_TRACKING_PATH))
                .withQueryParam(PARAM_WAYBILL, equalTo(WAYBILL_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"carrierId\":\"DPD\",\"waybills\":[{\"waybill\":\"" + WAYBILL_ONE
                                + "\",\"trackingDetails\":{\"statuses\":[{\"code\":\"" + TRACKING_CODE
                                + "\",\"description\":\"Delivered\","
                                + "\"occurredAt\":\"2026-01-01T00:00:00Z\"}],"
                                + "\"updatedAt\":\"2026-01-01T00:00:00Z\"}}]}")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            CarrierTracking tracking = allegro.orders().carrierTracking(CARRIER_ID, WAYBILL_ONE);

            // then
            assertEquals(CARRIER_ID, tracking.carrierId());
            assertEquals(1, tracking.waybills().size());
            assertEquals(TRACKING_CODE, tracking.waybills().get(0).statuses().get(0).code());
            verify(1, getRequestedFor(urlPathEqualTo(CARRIER_TRACKING_PATH))
                    .withQueryParam(PARAM_WAYBILL, equalTo(WAYBILL_ONE)));
        }
    }

    @Test
    void allegroPickupPoints_whenCarrierFilter_sendsCarriersQueryAndMapsPoints(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PICKUP_POINTS_PATH))
                .withQueryParam(PARAM_CARRIERS, equalTo(POINT_CARRIER))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"points\":[{\"id\":\"" + POINT_ID + "\",\"name\":\"Point 1\","
                                + "\"type\":\"PUDO\",\"description\":\"desc\","
                                + "\"address\":{\"street\":\"Main 1\",\"postCode\":\"00-001\","
                                + "\"city\":\"" + POINT_CITY + "\",\"countryCode\":\"PL\"}}]}")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PickupPoint> points = allegro.orders().allegroPickupPoints(
                    PointsFilter.ofCarriers(POINT_CARRIER));

            // then
            assertEquals(1, points.size());
            assertEquals(POINT_ID, points.get(0).id());
            assertEquals(POINT_CITY, points.get(0).address().city());
            verify(1, getRequestedFor(urlPathEqualTo(PICKUP_POINTS_PATH))
                    .withQueryParam(PARAM_CARRIERS, equalTo(POINT_CARRIER)));
        }
    }

    @Test
    void get_whenEnumValuesAreUnknownFutureValues_mapToUnknownSentinel(WireMockRuntimeInfo wmInfo) {
        // given — the order carries a buyer status and a fulfillment status this
        // SDK release does not model (a spec value added after this release)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_ENUM_ORDER_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — an unknown enum value must not fail the whole response
            Order order = allegro.orders().get(ORDER_ID);

            // then — both enums degrade to the forward-compat UNKNOWN sentinel
            assertEquals(OrderStatus.UNKNOWN, order.status());
            assertEquals(SellerStatus.UNKNOWN, order.sellerStatus());
        }
    }

    @Test
    void get_whenPaymentEnumsAreUnknownFutureValues_mapToUnknownSentinel(WireMockRuntimeInfo wmInfo) {
        // given — the payment carries a type and provider this SDK release does not model
        stubToken(TEST_TOKEN);
        String body = "{\"id\":\"" + ORDER_ID + "\",\"status\":\"BOUGHT\","
                + "\"buyer\":{\"id\":\"1\",\"login\":\"b\",\"email\":\"b@example.com\"},"
                + "\"lineItems\":[],"
                + "\"payment\":{\"type\":\"" + FUTURE_PAYMENT_TYPE + "\",\"provider\":\""
                + FUTURE_PAYMENT_PROVIDER + "\"},"
                + "\"summary\":{\"totalToPay\":{\"amount\":\"0.00\",\"currency\":\"PLN\"}}}";
        stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(body)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Order order = allegro.orders().get(ORDER_ID);

            // then — the unknown payment enums degrade to UNKNOWN, the order still maps
            assertEquals(PaymentType.UNKNOWN, order.payment().type());
            assertEquals(PaymentProvider.UNKNOWN, order.payment().provider());
        }
    }

    @Test
    void streamEvents_whenEventTypeIsUnknownFutureValue_mapsToUnknownSentinel(WireMockRuntimeInfo wmInfo) {
        // given — an event whose type this SDK release does not model
        stubToken(TEST_TOKEN);
        String eventBody = "{\"events\":[{\"id\":\"" + EVENT_ID_ONE + "\",\"type\":\""
                + FUTURE_EVENT_TYPE + "\",\"occurredAt\":\"" + EVENT_TIME + "\","
                + "\"order\":{\"checkoutForm\":{\"id\":\"" + EVENT_ORDER_ID_PREFIX + EVENT_ID_ONE
                + "\"}}}]}";
        stubFor(get(urlPathEqualTo(EVENTS_PATH)).withQueryParam(PARAM_FROM, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(eventBody)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<OrderEvent> events =
                    allegro.orders().streamEvents(OrderEventFilter.all()).limit(1).toList();

            // then — the unknown type degrades to UNKNOWN, the event still maps
            assertEquals(1, events.size());
            assertEquals(OrderEventType.UNKNOWN, events.get(0).type());
        }
    }

    @Test
    void streamOrders_whenFilterHasUnknownStatuses_omitsThemFromQuery(WireMockRuntimeInfo wmInfo) {
        // given — a filter carrying only the read-only UNKNOWN sentinels plus a real
        // (non-enum) filter; the sentinels must never be serialized to the wire
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ordersPage(1, 0))));
        OrderFilter filter = OrderFilter.builder()
                .statuses(OrderStatus.UNKNOWN)
                .fulfillmentStatuses(SellerStatus.UNKNOWN)
                .buyerLogin(EXPECTED_BUYER_LOGIN)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().streamOrders(filter).limit(1).toList();

            // then — UNKNOWN dropped (would 400 as status=UNKNOWN), real filter kept
            verify(1, getRequestedFor(urlPathEqualTo(CHECKOUT_FORMS_PATH))
                    .withQueryParam(PARAM_STATUS, absent())
                    .withQueryParam(PARAM_FULFILLMENT_STATUS, absent())
                    .withQueryParam(PARAM_BUYER_LOGIN, equalTo(EXPECTED_BUYER_LOGIN)));
        }
    }

    @Test
    void streamEvents_whenFilterHasUnknownType_omitsTypeFromQuery(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(eventsBody(EVENT_ID_ONE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — the UNKNOWN sentinel is not a real event type filter
            allegro.orders().streamEvents(OrderEventFilter.ofTypes(OrderEventType.UNKNOWN))
                    .limit(1).toList();

            // then — no type query param sent (dropped rather than type=UNKNOWN)
            verify(1, getRequestedFor(urlPathEqualTo(EVENTS_PATH))
                    .withQueryParam(PARAM_TYPE, absent()));
        }
    }
}
