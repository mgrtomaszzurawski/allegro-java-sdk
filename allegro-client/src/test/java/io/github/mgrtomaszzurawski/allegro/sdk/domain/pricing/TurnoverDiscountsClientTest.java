/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverThreshold;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for {@link TurnoverDiscounts}: the bare-array list response
 * (with a {@code null} element that must be skipped), the marketplace filter
 * query, the PUT request body for a set, the deactivate path, and a
 * representative 400/401 pair from the shared error handling.
 */
@WireMockTest
class TurnoverDiscountsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String TURNOVER_PATH = "/sale/turnover-discount";
    private static final String TEST_MARKETPLACE_ID = "allegro-pl";
    private static final String MARKETPLACE_PATH = TURNOVER_PATH + "/" + TEST_MARKETPLACE_ID;
    private static final String DEACTIVATE_PATH = MARKETPLACE_PATH + "/deactivate";
    private static final String LIST_FILTERED_URL = TURNOVER_PATH + "?marketplaceId=" + TEST_MARKETPLACE_ID;

    private static final String TURNOVER_AMOUNT = "1000.00";
    private static final String TEST_CURRENCY = "PLN";
    private static final String DISCOUNT_PERCENTAGE = "5";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long TEST_RETRY_AFTER = 1L;
    private static final String ERROR_CODE_VALIDATION = "ValidationError";
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Not found","userMessage":"Not found","path":null}]}
            """;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified (single active discount with one threshold)
    private static final String DISCOUNT_ACTIVE_RESPONSE = """
            {"marketplaceId":"allegro-pl","status":"ACTIVE","definitions":[
              {"cumulatingFromDate":"2026-01-01","spendingFromDate":"2026-02-01",
               "createdAt":"2026-07-17T10:15:30Z","updatedAt":"2026-07-17T10:15:30Z",
               "thresholds":[{"minimumTurnover":{"amount":"1000.00","currency":"PLN"},
                 "discount":{"percentage":"5"}}]}]}
            """;
    // spec-derived: not yet wire-verified (the array carries a null element)
    private static final String LIST_WITH_NULL_RESPONSE = """
            [null,{"marketplaceId":"allegro-pl","status":"ACTIVE","definitions":[
              {"cumulatingFromDate":"2026-01-01","spendingFromDate":"2026-02-01",
               "thresholds":[{"minimumTurnover":{"amount":"1000.00","currency":"PLN"},
                 "discount":{"percentage":"5"}}]}]}]
            """;
    // spec-derived: not yet wire-verified
    private static final String DEACTIVATING_RESPONSE = """
            {"marketplaceId":"allegro-pl","status":"DEACTIVATING","definitions":[]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String VALIDATION_ERROR_RESPONSE = """
            {"errors":[{"code":"ValidationError","message":"Company account required",
              "userMessage":"Company account required","path":"thresholds","details":null,"metadata":null}]}
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
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static TurnoverDiscountRequest request() {
        return TurnoverDiscountRequest.builder()
                .addThreshold(new TurnoverThreshold(
                        Money.of(TURNOVER_AMOUNT, TEST_CURRENCY), DISCOUNT_PERCENTAGE))
                .build();
    }

    @Test
    void list_whenArrayContainsNullElement_skipsNullAndMapsRest(WireMockRuntimeInfo wmInfo) {
        // given — a bare JSON array whose first element is null
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TURNOVER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(LIST_WITH_NULL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<TurnoverDiscount> discounts = allegro.pricing().turnoverDiscounts().list();

            // then — the null element is skipped, the real one is mapped
            assertEquals(1, discounts.size());
            TurnoverDiscount discount = discounts.get(0);
            assertEquals(TEST_MARKETPLACE_ID, discount.marketplaceId());
            assertEquals(TurnoverDiscount.Status.ACTIVE, discount.status());
            assertEquals(1, discount.definitions().size());
            TurnoverThreshold threshold = discount.definitions().get(0).thresholds().get(0);
            assertEquals(Money.of(TURNOVER_AMOUNT, TEST_CURRENCY), threshold.minimumTurnover());
            assertEquals(DISCOUNT_PERCENTAGE, threshold.discountPercentage());
            verify(1, getRequestedFor(urlEqualTo(TURNOVER_PATH)));
        }
    }

    @Test
    void list_withMarketplaceId_sendsMarketplaceIdQuery(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(LIST_FILTERED_URL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("[" + DISCOUNT_ACTIVE_RESPONSE + "]")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<TurnoverDiscount> discounts =
                    allegro.pricing().turnoverDiscounts().list(TEST_MARKETPLACE_ID);

            // then
            assertEquals(1, discounts.size());
            verify(1, getRequestedFor(urlEqualTo(LIST_FILTERED_URL)));
        }
    }

    @Test
    void set_whenValidRequest_putsThresholdsAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(MARKETPLACE_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(
                        "$.thresholds[0].minimumTurnover.amount", equalTo(TURNOVER_AMOUNT)))
                .withRequestBody(matchingJsonPath(
                        "$.thresholds[0].minimumTurnover.currency", equalTo(TEST_CURRENCY)))
                .withRequestBody(matchingJsonPath(
                        "$.thresholds[0].discount.percentage", equalTo(DISCOUNT_PERCENTAGE)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DISCOUNT_ACTIVE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            TurnoverDiscount discount =
                    allegro.pricing().turnoverDiscounts().set(TEST_MARKETPLACE_ID, request());

            // then
            assertEquals(TEST_MARKETPLACE_ID, discount.marketplaceId());
            assertEquals(TurnoverDiscount.Status.ACTIVE, discount.status());
            verify(1, putRequestedFor(urlEqualTo(MARKETPLACE_PATH)));
        }
    }

    @Test
    void deactivate_sendsPutToDeactivatePathAndMapsStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(DEACTIVATE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEACTIVATING_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            TurnoverDiscount discount =
                    allegro.pricing().turnoverDiscounts().deactivate(TEST_MARKETPLACE_ID);

            // then
            assertEquals(TurnoverDiscount.Status.DEACTIVATING, discount.status());
            verify(1, putRequestedFor(urlEqualTo(DEACTIVATE_PATH)));
        }
    }

    @Test
    void set_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(MARKETPLACE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(VALIDATION_ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            TurnoverDiscounts turnover = allegro.pricing().turnoverDiscounts();
            TurnoverDiscountRequest request = request();

            // then
            AllegroBadRequestException failure = assertThrows(
                    AllegroBadRequestException.class,
                    () -> turnover.set(TEST_MARKETPLACE_ID, request));
            assertEquals(1, failure.errors().size());
            assertEquals(ERROR_CODE_VALIDATION, failure.errors().get(0).code());
            assertEquals("thresholds", failure.errors().get(0).path());
        }
    }

    @Test
    void deactivate_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given — deactivating a marketplace with no discount
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(DEACTIVATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            TurnoverDiscounts turnover = allegro.pricing().turnoverDiscounts();

            // then
            AllegroNotFoundException failure = assertThrows(
                    AllegroNotFoundException.class, () -> turnover.deactivate(TEST_MARKETPLACE_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
        }
    }

    @Test
    void list_when429_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — every attempt is throttled; the policy allows one retry
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TURNOVER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(TEST_RETRY_AFTER))));

        try (AllegroClient allegro = client(wmInfo)) {
            TurnoverDiscounts turnover = allegro.pricing().turnoverDiscounts();

            // then — retried once (verify 2), then surfaced with Retry-After
            AllegroRateLimitException failure = assertThrows(
                    AllegroRateLimitException.class, turnover::list);
            assertEquals(TEST_RETRY_AFTER, failure.retryAfterSeconds());
            verify(2, getRequestedFor(urlEqualTo(TURNOVER_PATH)));
        }
    }

    @Test
    void list_when5xx_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given — a GET is retried once, then the server error surfaces
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TURNOVER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            TurnoverDiscounts turnover = allegro.pricing().turnoverDiscounts();

            // then
            assertThrows(AllegroServerException.class, turnover::list);
            verify(2, getRequestedFor(urlEqualTo(TURNOVER_PATH)));
        }
    }

    @Test
    void list_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(TURNOVER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(TURNOVER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("[" + DISCOUNT_ACTIVE_RESPONSE + "]")));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<TurnoverDiscount> discounts = allegro.pricing().turnoverDiscounts().list();

            // then
            assertEquals(1, discounts.size());
            verify(2, getRequestedFor(urlEqualTo(TURNOVER_PATH)));
        }
    }
}
