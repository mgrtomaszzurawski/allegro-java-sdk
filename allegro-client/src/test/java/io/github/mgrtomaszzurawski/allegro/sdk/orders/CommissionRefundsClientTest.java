/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ClaimFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RefundClaimRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.RefundClaim;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the commission-refunds sub-facade: lazy claim
 * streaming, the claim write (body verified, POST not retried), cancel, and
 * representative error routing.
 */
@WireMockTest
class CommissionRefundsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String CLAIMS_PATH = "/order/refund-claims";
    private static final String CLAIM_UUID = "00000000-0000-0000-0000-000000000001";
    private static final String CLAIM_PATH = CLAIMS_PATH + "/" + CLAIM_UUID;
    private static final String NEW_CLAIM_ID = "claim-1";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_OFFER_ID = "lineItem.offer.id";
    private static final String LINE_ITEM_ID = "li-1";
    private static final String OFFER_ID = "12345";
    private static final int QUANTITY = 2;

    private static final int PAGE_SIZE = 100;
    private static final int SHORT_PAGE = 3;
    private static final String OFFSET_PAGE_TWO = String.valueOf(PAGE_SIZE);

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String ERRORS_BODY = """
            {"errors":[{"code":"InvalidInput","message":"bad","path":"lineItem.id"}]}
            """;
    // spec-derived: not yet wire-verified.
    private static final String CLAIM_BODY = "{\"id\":\"" + CLAIM_UUID + "\",\"quantity\":1,"
            + "\"commission\":{\"amount\":\"1.50\",\"currency\":\"PLN\"},"
            + "\"buyer\":{\"id\":\"44556677\"},\"createdAt\":\"2026-01-01T00:00:00Z\","
            + "\"lineItem\":{\"id\":\"" + LINE_ITEM_ID + "\",\"offer\":{\"id\":\"" + OFFER_ID + "\"}}}";
    private static final String NEW_CLAIM_ID_BODY = "{\"id\":\"" + NEW_CLAIM_ID + "\"}";

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

    private static String claimsPage(int count) {
        StringBuilder claims = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                claims.append(',');
            }
            claims.append(CLAIM_BODY);
        }
        return "{\"refundClaims\":[" + claims + "],\"count\":" + count + "}";
    }

    @Test
    void streamClaims_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(CLAIMS_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(claimsPage(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<RefundClaim> firstOnly = allegro.orders().commissionRefunds()
                    .streamClaims(ClaimFilter.all()).limit(1).toList();

            // then — mapped, page two never fetched
            assertEquals(1, firstOnly.size());
            assertEquals(LINE_ITEM_ID, firstOnly.get(0).lineItemId());
            assertEquals(OFFER_ID, firstOnly.get(0).offerId());
            verify(0, getRequestedFor(urlPathEqualTo(CLAIMS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamClaims_whenFilterGiven_terminatesOnShortPageAndSendsQuery(WireMockRuntimeInfo wmInfo) {
        // given — a short page ends the walk
        stubToken();
        stubFor(get(urlPathEqualTo(CLAIMS_PATH)).withQueryParam(PARAM_OFFER_ID, equalTo(OFFER_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(claimsPage(SHORT_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.orders().commissionRefunds()
                    .streamClaims(ClaimFilter.builder().offerId(OFFER_ID).build()).count();

            // then
            assertEquals(SHORT_PAGE, total);
            verify(getRequestedFor(urlPathEqualTo(CLAIMS_PATH))
                    .withQueryParam(PARAM_OFFER_ID, equalTo(OFFER_ID)));
        }
    }

    @Test
    void get_whenCalled_mapsClaim(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(CLAIM_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CLAIM_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            RefundClaim claim = allegro.orders().commissionRefunds().get(CLAIM_UUID);

            // then
            assertEquals(CLAIM_UUID, claim.id());
            assertEquals(LINE_ITEM_ID, claim.lineItemId());
            assertEquals("1.50", claim.commission().amount());
        }
    }

    @Test
    void claim_whenCalled_postsClaimBodyAndReturnsId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(CLAIMS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(NEW_CLAIM_ID_BODY)));
        RefundClaimRequest request = RefundClaimRequest.builder()
                .lineItemId(LINE_ITEM_ID).quantity(QUANTITY).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            String newId = allegro.orders().commissionRefunds().claim(request);

            // then
            assertEquals(NEW_CLAIM_ID, newId);
            verify(1, postRequestedFor(urlEqualTo(CLAIMS_PATH))
                    .withRequestBody(matchingJsonPath("$.lineItem.id", equalTo(LINE_ITEM_ID)))
                    .withRequestBody(matchingJsonPath("$.quantity", equalTo(String.valueOf(QUANTITY)))));
        }
    }

    @Test
    void claim_when5xx_doesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(CLAIMS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RefundClaimRequest request = RefundClaimRequest.builder()
                .lineItemId(LINE_ITEM_ID).quantity(QUANTITY).build();

        try (AllegroClient allegro = client(wmInfo)) {
            var commissionRefunds = allegro.orders().commissionRefunds();

            // then
            assertThrows(AllegroServerException.class, () -> commissionRefunds.claim(request));
            verify(1, postRequestedFor(urlEqualTo(CLAIMS_PATH)));
        }
    }

    @Test
    void cancel_whenCalled_deletesClaim(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(delete(urlEqualTo(CLAIM_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.orders().commissionRefunds().cancel(CLAIM_UUID);

            // then
            verify(1, deleteRequestedFor(urlEqualTo(CLAIM_PATH)));
        }
    }

    @Test
    void get_when400WithErrors_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(CLAIM_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var commissionRefunds = allegro.orders().commissionRefunds();

            // then
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> commissionRefunds.get(CLAIM_UUID));
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals("lineItem.id", failure.errors().get(0).path());
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(CLAIM_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var commissionRefunds = allegro.orders().commissionRefunds();

            // then
            assertThrows(AllegroNotFoundException.class, () -> commissionRefunds.get(CLAIM_UUID));
        }
    }
}
