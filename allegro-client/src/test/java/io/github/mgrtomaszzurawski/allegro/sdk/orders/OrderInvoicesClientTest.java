/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
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
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.InvoiceDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderInvoice;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the order-invoices sub-facade: list mapping, the
 * declare write (body verified), the binary file upload, and the mandatory
 * error-path table (400 typed / 401-replay / 404 / 429 / 5xx).
 */
@WireMockTest
class OrderInvoicesClientTest {

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

    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final String INVOICES_PATH = "/order/checkout-forms/" + ORDER_ID + "/invoices";
    private static final String INVOICE_ID = "inv-1";
    private static final String FILE_PATH = INVOICES_PATH + "/" + INVOICE_ID + "/file";
    private static final String INVOICE_NUMBER = "FV/2026/01";
    private static final String FILE_NAME = "invoice.pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private static final long RETRY_AFTER_SECONDS = 30L;
    private static final int FAST_MAX_ATTEMPTS = 2;
    private static final int EXPECTED_REQUESTS_WITH_ONE_REPLAY = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String ERRORS_BODY = """
            {"errors":[{"code":"InvalidInput","message":"bad","path":"invoiceNumber"}]}
            """;
    // spec-derived: not yet wire-verified (order-keyed; needs a seeded order).
    private static final String INVOICES_BODY = "{\"invoices\":[{\"id\":\"" + INVOICE_ID
            + "\",\"invoiceNumber\":\"" + INVOICE_NUMBER + "\","
            + "\"createdAt\":\"2026-01-01T00:00:00Z\",\"file\":{\"name\":\"" + FILE_NAME + "\"}}]}";
    private static final String NEW_INVOICE_ID_BODY = "{\"id\":\"" + INVOICE_ID + "\"}";

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

    private static InvoiceDeclaration declaration() {
        return InvoiceDeclaration.builder().invoiceNumber(INVOICE_NUMBER).fileName(FILE_NAME).build();
    }

    @Test
    void ofOrder_whenCalled_mapsInvoices(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(INVOICES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(INVOICES_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<OrderInvoice> invoices = allegro.orders().invoices().ofOrder(ORDER_ID);

            // then
            assertEquals(1, invoices.size());
            assertEquals(INVOICE_ID, invoices.get(0).id());
            assertEquals(INVOICE_NUMBER, invoices.get(0).invoiceNumber());
            assertEquals(FILE_NAME, invoices.get(0).fileName());
        }
    }

    @Test
    void declare_whenCalled_postsMetadataAndReturnsId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(INVOICES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(NEW_INVOICE_ID_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            String newId = allegro.orders().invoices().declare(ORDER_ID, declaration());

            // then — id returned, request body carried the metadata
            assertEquals(INVOICE_ID, newId);
            verify(1, postRequestedFor(urlEqualTo(INVOICES_PATH))
                    .withRequestBody(matchingJsonPath("$.invoiceNumber", equalTo(INVOICE_NUMBER)))
                    .withRequestBody(matchingJsonPath("$.file.name", equalTo(FILE_NAME))));
        }
    }

    @Test
    void uploadFile_whenCalled_putsBinaryBodyWithPdfContentType(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(FILE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        byte[] pdfBytes = "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8);

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().invoices().uploadFile(ORDER_ID, INVOICE_ID, pdfBytes);

            // then — the raw bytes reached the wire with the file content type
            verify(1, putRequestedFor(urlEqualTo(FILE_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(PDF_CONTENT_TYPE))
                    .withRequestBody(binaryEqualTo(pdfBytes)));
        }
    }

    @Test
    void ofOrder_when400WithErrors_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(INVOICES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var invoices = allegro.orders().invoices();

            // then
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> invoices.ofOrder(ORDER_ID));
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals("invoiceNumber", failure.errors().get(0).path());
        }
    }

    @Test
    void ofOrder_when401Once_reauthenticatesAndReplays(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(INVOICES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(INVOICES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(INVOICES_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().invoices().ofOrder(ORDER_ID);

            // then
            verify(EXPECTED_REQUESTS_WITH_ONE_REPLAY, getRequestedFor(urlEqualTo(INVOICES_PATH)));
            verify(1, getRequestedFor(urlEqualTo(INVOICES_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void ofOrder_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(INVOICES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var invoices = allegro.orders().invoices();

            // then
            assertThrows(AllegroNotFoundException.class, () -> invoices.ofOrder(ORDER_ID));
        }
    }

    @Test
    void ofOrder_when429Exhausted_throwsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(INVOICES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, Long.toString(RETRY_AFTER_SECONDS))
                        .withBody(ERRORS_BODY)));
        RetryPolicy fast = RetryPolicy.builder().maxAttempts(FAST_MAX_ATTEMPTS)
                .maxRetryAfterSeconds(0L).build();

        try (AllegroClient allegro = client(wmInfo, fast)) {
            var invoices = allegro.orders().invoices();

            // then
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, () -> invoices.ofOrder(ORDER_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(INVOICES_PATH)));
        }
    }

    @Test
    void ofOrder_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(INVOICES_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(INVOICES_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(INVOICES_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<OrderInvoice> invoices = allegro.orders().invoices().ofOrder(ORDER_ID);

            // then
            assertEquals(1, invoices.size());
            verify(EXPECTED_REQUESTS_WITH_ONE_REPLAY, getRequestedFor(urlEqualTo(INVOICES_PATH)));
        }
    }
}
