/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the orders facade starter slice
 * ({@code orders().get(id)}): happy-path mapping of the flagship order record
 * plus the mandatory error-path table (400 typed / 401 replay / 404 / 429 / 5xx).
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
    private static final String ORDER_PATH = "/order/checkout-forms/" + ORDER_ID;
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

    private static final long RETRY_AFTER_SECONDS = 120L;
    private static final int FAST_MAX_ATTEMPTS = 2;
    // Original request + one transparent replay/retry = two wire requests.
    private static final int EXPECTED_REQUESTS_WITH_ONE_REPLAY = 2;
    private static final int EXPECTED_REQUESTS_WITH_ONE_RETRY = 2;

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
            assertTrue(order.lineItems().isEmpty());
            // and optional buyer fields map to null, guest defaults to false
            assertNull(order.buyer().firstName());
            assertNull(order.buyer().lastName());
            assertNull(order.buyer().companyName());
            assertNull(order.buyer().phoneNumber());
            assertFalse(order.buyer().guest());
        }
    }
}
