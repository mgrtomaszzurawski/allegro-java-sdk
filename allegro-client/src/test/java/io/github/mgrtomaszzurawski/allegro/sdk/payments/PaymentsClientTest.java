/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.payments;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.PaymentOperationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentRefund;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.RefundReason;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the payments facade: lazy operation/refund
 * streaming (offset pagination), the refund write (request body verified on the
 * wire, POST not retried), and the error-path table.
 */
@WireMockTest
class PaymentsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String OPERATIONS_PATH = "/payments/payment-operations";
    private static final String REFUNDS_PATH = "/payments/refunds";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_ORDER_ID = "order.id";

    private static final int PAGE_SIZE = 100;
    private static final int SECOND_PAGE = 5;
    private static final int TOTAL_OPERATIONS = PAGE_SIZE + SECOND_PAGE;
    private static final String OFFSET_PAGE_TWO = String.valueOf(PAGE_SIZE);

    private static final String PAYMENT_ID = "0f3e2b1a-1111-2222-3333-444455556666";
    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final String COMMAND_ID = "b1c2d3e4-1111-2222-3333-444455556666";
    private static final String REFUND_ID = "c1d2e3f4-1111-2222-3333-444455556666";
    private static final String REFUND_STATUS_NEW = "NEW";

    private static final long RETRY_AFTER_SECONDS = 30L;
    private static final int FAST_MAX_ATTEMPTS = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String ERRORS_BODY = """
            {"errors":[{"code":"InvalidQuery","message":"bad","path":"payment.id"}]}
            """;
    private static final String REFUND_BODY = "{\"id\":\"" + REFUND_ID + "\","
            + "\"payment\":{\"id\":\"" + PAYMENT_ID + "\"},"
            + "\"status\":\"" + REFUND_STATUS_NEW + "\","
            + "\"createdAt\":\"2026-01-01T00:00:00Z\","
            + "\"totalValue\":{\"amount\":\"10.00\",\"currency\":\"PLN\"}}";

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

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    // A PAYOUT operation — a registered @JsonSubTypes discriminator, so the
    // polymorphic BaseOperation element deserializes; only common fields are read.
    private static String operationJson() {
        return "{\"type\":\"PAYOUT\",\"group\":\"OUTCOME\","
                + "\"value\":{\"amount\":\"-25.00\",\"currency\":\"PLN\"},"
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"marketplaceId\":\"allegro-pl\"}";
    }

    private static String operationsPage(int count) {
        StringBuilder operations = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                operations.append(',');
            }
            operations.append(operationJson());
        }
        return "{\"paymentOperations\":[" + operations + "],\"count\":" + count
                + ",\"totalCount\":" + TOTAL_OPERATIONS + "}";
    }

    @Test
    void streamOperations_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(OPERATIONS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationsPage(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PaymentOperation> firstOnly = allegro.payments()
                    .streamOperations(PaymentOperationFilter.all()).limit(1).toList();

            // then — first operation mapped, page two never fetched
            assertEquals(1, firstOnly.size());
            assertEquals("PAYOUT", firstOnly.get(0).type());
            assertEquals("OUTCOME", firstOnly.get(0).group());
            verify(0, getRequestedFor(urlPathEqualTo(OPERATIONS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamOperations_whenTraversed_fetchesAllPagesUsingTotalCount(WireMockRuntimeInfo wmInfo) {
        // given — full page then a page that reaches totalCount
        stubToken();
        stubFor(get(urlPathEqualTo(OPERATIONS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationsPage(PAGE_SIZE))));
        stubFor(get(urlPathEqualTo(OPERATIONS_PATH)).withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationsPage(SECOND_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.payments().streamOperations(PaymentOperationFilter.all()).count();

            // then
            assertEquals(TOTAL_OPERATIONS, total);
            verify(1, getRequestedFor(urlPathEqualTo(OPERATIONS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamOperations_when400WithErrors_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(OPERATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var payments = allegro.payments();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> payments.streamOperations(PaymentOperationFilter.all()).toList());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void streamOperations_when429Exhausted_throwsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(OPERATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                Long.toString(RETRY_AFTER_SECONDS))
                        .withBody(ERRORS_BODY)));
        RetryPolicy fast = RetryPolicy.builder()
                .maxAttempts(FAST_MAX_ATTEMPTS).maxRetryAfterSeconds(0L).build();

        try (AllegroClient allegro = client(wmInfo, fast)) {
            var payments = allegro.payments();

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> payments.streamOperations(PaymentOperationFilter.all()).toList());
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
        }
    }

    @Test
    void streamRefunds_whenFilterGiven_mapsAndSendsQueryParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(REFUNDS_PATH)).withQueryParam(PARAM_ORDER_ID, equalTo(ORDER_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"refunds\":[" + REFUND_BODY + "],\"count\":1,\"totalCount\":1}")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PaymentRefund> refunds = allegro.payments()
                    .streamRefunds(RefundFilter.builder().orderId(ORDER_ID).build()).toList();

            // then
            assertEquals(1, refunds.size());
            assertEquals(REFUND_ID, refunds.get(0).id());
            assertEquals(REFUND_STATUS_NEW, refunds.get(0).status());
            assertEquals(PAYMENT_ID, refunds.get(0).paymentId());
            verify(getRequestedFor(urlPathEqualTo(REFUNDS_PATH))
                    .withQueryParam(PARAM_ORDER_ID, equalTo(ORDER_ID)));
        }
    }

    @Test
    void refund_whenCalled_postsInitializeRefundBodyAndMapsResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlPathEqualTo(REFUNDS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(REFUND_BODY)));
        RefundRequest request = RefundRequest.builder()
                .paymentId(PAYMENT_ID).orderId(ORDER_ID)
                .commandId(COMMAND_ID).reason(RefundReason.COMPLAINT)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PaymentRefund refund = allegro.payments().refund(request);

            // then — response mapped, and the request body reached the wire
            assertEquals(REFUND_ID, refund.id());
            assertEquals(REFUND_STATUS_NEW, refund.status());
            verify(1, postRequestedFor(urlPathEqualTo(REFUNDS_PATH))
                    .withRequestBody(matchingJsonPath("$.payment.id", equalTo(PAYMENT_ID)))
                    .withRequestBody(matchingJsonPath("$.order.id", equalTo(ORDER_ID)))
                    .withRequestBody(matchingJsonPath("$.commandId", equalTo(COMMAND_ID)))
                    .withRequestBody(matchingJsonPath("$.reason",
                            equalTo(RefundReason.COMPLAINT.name()))));
        }
    }

    @Test
    void refund_when5xx_doesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given — a POST is not retried by default
        stubToken();
        stubFor(post(urlPathEqualTo(REFUNDS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RefundRequest request = RefundRequest.builder()
                .paymentId(PAYMENT_ID).orderId(ORDER_ID)
                .commandId(COMMAND_ID).reason(RefundReason.REFUND)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            var payments = allegro.payments();

            // then
            assertThrows(AllegroServerException.class, () -> payments.refund(request));
            verify(1, postRequestedFor(urlPathEqualTo(REFUNDS_PATH)));
        }
    }

    @Test
    void payments_whenClientClosed_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given
        AllegroClient allegro = client(wmInfo);
        allegro.close();

        // then
        assertThrows(IllegalStateException.class, allegro::payments);
    }
}
