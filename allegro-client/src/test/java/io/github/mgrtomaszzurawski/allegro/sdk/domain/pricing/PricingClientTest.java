/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the root {@link Pricing} facade reads: the repeated
 * {@code offer.id} query on {@code quotes(...)}, the deposit-type mapping, and a
 * representative 401-replay proving these GETs share the SDK's auth handling.
 */
@WireMockTest
class PricingClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String OFFER_ID_ONE = "111";
    private static final String OFFER_ID_TWO = "222";
    private static final String QUOTES_URL =
            "/pricing/offer-quotes?offer.id=" + OFFER_ID_ONE + "&offer.id=" + OFFER_ID_TWO;
    private static final String DEPOSIT_TYPES_PATH = "/deposit/types";

    private static final String TEST_CURRENCY = "PLN";
    private static final String QUOTE_TYPE = "PROMO";
    private static final String QUOTE_NAME = "Promoted listing";
    private static final String QUOTE_FEE_AMOUNT = "1.23";
    private static final Instant QUOTE_NEXT_DATE = Instant.parse("2026-08-01T00:00:00Z");
    private static final String DEPOSIT_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String DEPOSIT_NAME = "Bottle deposit";
    private static final String DEPOSIT_MARKETPLACE = "allegro-pl";
    private static final String DEPOSIT_PRICE_AMOUNT = "0.50";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified
    private static final String QUOTES_RESPONSE = """
            {"count":1,"quotes":[
              {"offer":{"id":"111"},"type":"PROMO","name":"Promoted listing","enabled":true,
               "fee":{"amount":"1.23","currency":"PLN"},"nextDate":"2026-08-01T00:00:00Z"}
            ]}
            """;
    // spec-derived: not yet wire-verified (empty result: the list may be null/absent)
    private static final String EMPTY_QUOTES_RESPONSE = """
            {"count":0}
            """;
    // spec-derived: not yet wire-verified
    private static final String DEPOSIT_TYPES_RESPONSE = """
            {"deposits":[
              {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"Bottle deposit",
               "marketplaceId":"allegro-pl","price":{"amount":"0.50","currency":"PLN"}}
            ]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(2)
                                .backoffStrategy(RetryPolicy.BackoffStrategy.FIXED)
                                .build())
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    @Test
    void quotes_whenOffersGiven_sendsRepeatedOfferIdAndMapsQuotes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(QUOTES_URL))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(QUOTES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — both offer ids are sent as repeated offer.id parameters
            List<OfferQuote> quotes =
                    allegro.pricing().quotes(List.of(OFFER_ID_ONE, OFFER_ID_TWO));

            // then
            assertEquals(1, quotes.size());
            OfferQuote quote = quotes.get(0);
            assertEquals(OFFER_ID_ONE, quote.offerId());
            assertEquals(QUOTE_TYPE, quote.type());
            assertEquals(QUOTE_NAME, quote.name());
            assertTrue(quote.enabled());
            assertEquals(Money.of(QUOTE_FEE_AMOUNT, TEST_CURRENCY), quote.fee());
            assertEquals(QUOTE_NEXT_DATE, quote.nextDate());
            verify(1, getRequestedFor(urlEqualTo(QUOTES_URL)));
        }
    }

    @Test
    void quotes_whenNoQuotesInResponse_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — the response omits the quotes array entirely
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(QUOTES_URL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(EMPTY_QUOTES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when / then
            List<OfferQuote> quotes =
                    allegro.pricing().quotes(List.of(OFFER_ID_ONE, OFFER_ID_TWO));
            assertTrue(quotes.isEmpty());
        }
    }

    @Test
    void depositTypes_whenTypesExist_mapsIdNameMarketplaceAndPrice(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(DEPOSIT_TYPES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEPOSIT_TYPES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<DepositType> depositTypes = allegro.pricing().depositTypes();

            // then
            assertEquals(1, depositTypes.size());
            DepositType deposit = depositTypes.get(0);
            assertEquals(UUID.fromString(DEPOSIT_ID), deposit.id());
            assertEquals(DEPOSIT_NAME, deposit.name());
            assertEquals(DEPOSIT_MARKETPLACE, deposit.marketplaceId());
            assertEquals(Money.of(DEPOSIT_PRICE_AMOUNT, TEST_CURRENCY), deposit.price());
            verify(1, getRequestedFor(urlEqualTo(DEPOSIT_TYPES_PATH)));
        }
    }

    @Test
    void depositTypes_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two
        stubFor(post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(DEPOSIT_TYPES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(DEPOSIT_TYPES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEPOSIT_TYPES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<DepositType> depositTypes = allegro.pricing().depositTypes();

            // then — replayed once, second request carried the fresh token
            assertEquals(1, depositTypes.size());
            verify(2, getRequestedFor(urlEqualTo(DEPOSIT_TYPES_PATH)));
            verify(1, getRequestedFor(urlEqualTo(DEPOSIT_TYPES_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }
}
