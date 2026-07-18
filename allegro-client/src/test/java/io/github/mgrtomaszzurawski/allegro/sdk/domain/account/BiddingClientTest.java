/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.MyBid;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * Facade test for {@code client.bidding()} — reading a bid and placing one,
 * with request-body verification and the 404 (no auction/no bid) and 422
 * (bidding not allowed) mappings.
 */
@WireMockTest
class BiddingClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final int HTTP_UNPROCESSABLE = 422;
    private static final String OFFER_ID = "12345678";
    private static final String BID_PATH = "/bidding/offers/" + OFFER_ID + "/bid";
    private static final String CURRENCY_PLN = "PLN";
    private static final String MAX_AMOUNT = "120.00";
    private static final String CURRENT_AMOUNT = "100.00";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String BID_RESPONSE = """
            {"maxAmount":{"amount":"%s","currency":"%s"},"minimalPriceMet":true,
             "highBidder":true,"auction":{"currentPrice":{"amount":"%s","currency":"%s"}}}
            """.formatted(MAX_AMOUNT, CURRENCY_PLN, CURRENT_AMOUNT, CURRENCY_PLN);
    // spec-derived: not yet wire-verified (422 "bidding not allowed" errors[] shape)
    private static final String NOT_ALLOWED_RESPONSE = """
            {"errors":[{"code":"BiddingNotAllowed","message":"cannot bid",
              "userMessage":"Licytacja niedozwolona","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    @Test
    void myBid_whenBidExists_mapsAmountsAndFlags(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(BID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BID_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            MyBid bid = allegro.bidding().myBid(OFFER_ID);

            // then — nested price objects mapped to Money, flags preserved
            assertEquals(Money.of(MAX_AMOUNT, CURRENCY_PLN), bid.maxAmount());
            assertEquals(Money.of(CURRENT_AMOUNT, CURRENCY_PLN), bid.currentPrice());
            assertTrue(bid.highBidder());
            assertEquals(Boolean.TRUE, bid.minimalPriceMet());
        }
    }

    @Test
    void placeBid_whenValid_putsMaxAmountBodyAndMapsResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(BID_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(containing("\"amount\":\"" + MAX_AMOUNT + "\""))
                .withRequestBody(containing("\"currency\":\"" + CURRENCY_PLN + "\""))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BID_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            MyBid bid = allegro.bidding().placeBid(OFFER_ID, Money.of(MAX_AMOUNT, CURRENCY_PLN));

            // then — maxAmount sent under a maxAmount wrapper, response mapped back
            assertEquals(Money.of(MAX_AMOUNT, CURRENCY_PLN), bid.maxAmount());
            verify(1, putRequestedFor(urlEqualTo(BID_PATH))
                    .withRequestBody(containing("\"maxAmount\"")));
        }
    }

    @Test
    void placeBid_when422BiddingNotAllowed_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(BID_PATH))
                .willReturn(aResponse().withStatus(HTTP_UNPROCESSABLE)
                        .withBody(NOT_ALLOWED_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Money maxAmount = Money.of(MAX_AMOUNT, CURRENCY_PLN);
            Bidding bidding = allegro.bidding();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> bidding.placeBid(OFFER_ID, maxAmount));
            assertEquals("BiddingNotAllowed", failure.errors().get(0).code());
        }
    }

    @Test
    void myBid_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given — 404 = auction not found OR user has not bid
        stubFor(get(urlEqualTo(BID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            Bidding bidding = allegro.bidding();

            // then
            assertThrows(AllegroNotFoundException.class, () -> bidding.myBid(OFFER_ID));
        }
    }
}
