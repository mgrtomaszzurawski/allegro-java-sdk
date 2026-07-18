/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RejectionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ReturnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.CustomerReturn;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.ReturnRejectionCode;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the customer-returns sub-facade (BETA): lazy
 * streaming with the beta {@code Accept} media type on the wire, the reject
 * write (body verified), and representative error routing.
 */
@WireMockTest
class CustomerReturnsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String RETURNS_PATH = "/order/customer-returns";
    private static final String RETURN_ID = "ret-1";
    private static final String RETURN_PATH = RETURNS_PATH + "/" + RETURN_ID;
    private static final String REJECTION_PATH = RETURN_PATH + "/rejection";
    private static final String PARAM_OFFSET = "offset";
    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final String BUYER_LOGIN = "test-buyer";
    private static final String REASON = "Repaired under warranty";

    private static final int PAGE_SIZE = 100;
    private static final int TOTAL_RETURNS = 150;
    private static final String OFFSET_PAGE_TWO = String.valueOf(PAGE_SIZE);

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String ERRORS_BODY = """
            {"errors":[{"code":"InvalidInput","message":"bad","path":"rejection.code"}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    // spec-derived: not yet wire-verified (order-keyed; needs a seeded buyer return).
    private static String returnJson(String id) {
        return "{\"id\":\"" + id + "\",\"orderId\":\"" + ORDER_ID + "\","
                + "\"referenceNumber\":\"R-1\",\"buyer\":{\"login\":\"" + BUYER_LOGIN
                + "\",\"email\":\"buyer@example.com\"},\"items\":[{}],"
                + "\"createdAt\":\"2026-01-01T00:00:00Z\",\"marketplaceId\":\"allegro-pl\"}";
    }

    private static String returnsPage(int count) {
        StringBuilder returns = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                returns.append(',');
            }
            returns.append(returnJson("ret-" + index));
        }
        return "{\"count\":" + TOTAL_RETURNS + ",\"customerReturns\":[" + returns + "]}";
    }

    @Test
    void streamReturns_whenConsumingFirstElement_doesNotFetchNextPageAndUsesBetaAccept(
            WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more (count > page)
        stubToken();
        stubFor(get(urlPathEqualTo(RETURNS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnsPage(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<CustomerReturn> firstOnly = allegro.orders().returns()
                    .streamReturns(ReturnFilter.all()).limit(1).toList();

            // then — mapped, beta Accept on the wire, page two never fetched
            assertEquals(1, firstOnly.size());
            assertEquals(ORDER_ID, firstOnly.get(0).orderId());
            assertEquals(BUYER_LOGIN, firstOnly.get(0).buyerLogin());
            verify(1, getRequestedFor(urlPathEqualTo(RETURNS_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1)));
            verify(0, getRequestedFor(urlPathEqualTo(RETURNS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamReturns_whenFilterGiven_carriesFilterAcrossPageBoundary(WireMockRuntimeInfo wmInfo) {
        // given — both pages require the orderId filter; a page-2 request dropping
        // it would miss the stub and the walk would fail
        stubToken();
        stubFor(get(urlPathEqualTo(RETURNS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .withQueryParam("orderId", equalTo(ORDER_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnsPage(PAGE_SIZE))));
        stubFor(get(urlPathEqualTo(RETURNS_PATH)).withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                .withQueryParam("orderId", equalTo(ORDER_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnsPage(TOTAL_RETURNS - PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.orders().returns()
                    .streamReturns(ReturnFilter.builder().orderId(ORDER_ID).build()).count();

            // then — page two carried the filter (else the stub would not match)
            assertEquals(TOTAL_RETURNS, total);
            verify(1, getRequestedFor(urlPathEqualTo(RETURNS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                    .withQueryParam("orderId", equalTo(ORDER_ID)));
        }
    }

    @Test
    void get_whenCalled_mapsReturnWithBetaAccept(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(RETURN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnJson(RETURN_ID))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            CustomerReturn customerReturn = allegro.orders().returns().get(RETURN_ID);

            // then
            assertEquals(RETURN_ID, customerReturn.id());
            assertEquals(1, customerReturn.itemCount());
            verify(1, getRequestedFor(urlEqualTo(RETURN_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1)));
        }
    }

    @Test
    void rejectRefund_whenCalled_postsRejectionBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(REJECTION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnJson(RETURN_ID))));
        RejectionRequest request = RejectionRequest.builder()
                .code(ReturnRejectionCode.ITEM_FIXED).reason(REASON).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            CustomerReturn rejected = allegro.orders().returns().rejectRefund(RETURN_ID, request);

            // then — the rejection code + reason reached the wire, and the beta
            // surface got BOTH the beta Accept and the beta request Content-Type
            // (the v1 content type is rejected on this beta write endpoint)
            assertEquals(RETURN_ID, rejected.id());
            verify(1, postRequestedFor(urlEqualTo(REJECTION_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                    .withRequestBody(matchingJsonPath("$.rejection.code",
                            equalTo(ReturnRejectionCode.ITEM_FIXED.name())))
                    .withRequestBody(matchingJsonPath("$.rejection.reason", equalTo(REASON))));
        }
    }

    @Test
    void streamReturns_when400WithErrors_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(RETURNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var returns = allegro.orders().returns();

            // then — the errors[] payload survives as typed field errors
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> returns.streamReturns(ReturnFilter.all()).toList());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals("rejection.code", failure.errors().get(0).path());
        }
    }

    @Test
    void rejectRefund_when5xx_doesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given — a POST is not retried by default, even on a transient 500
        stubToken();
        stubFor(post(urlEqualTo(REJECTION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RejectionRequest request = RejectionRequest.builder()
                .code(ReturnRejectionCode.ITEM_FIXED).reason(REASON).build();

        try (AllegroClient allegro = client(wmInfo)) {
            var returns = allegro.orders().returns();

            // then — one attempt only
            assertThrows(AllegroServerException.class,
                    () -> returns.rejectRefund(RETURN_ID, request));
            verify(1, postRequestedFor(urlEqualTo(REJECTION_PATH)));
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(RETURN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var returns = allegro.orders().returns();

            // then
            assertThrows(AllegroNotFoundException.class, () -> returns.get(RETURN_ID));
        }
    }
}
