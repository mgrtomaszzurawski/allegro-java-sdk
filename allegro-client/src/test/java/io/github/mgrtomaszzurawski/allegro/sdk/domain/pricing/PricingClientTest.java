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
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ClassifiedsExtraPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ClassifiedsPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreviewSellingMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
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

    private static final String FEE_PREVIEW_PATH = "/pricing/offer-fee-preview";
    private static final String TEST_CATEGORY_ID = "257";
    private static final String PRICE_AMOUNT = "99.99";
    private static final String FEE_OFFER_ID = "654321";
    private static final String COMMISSION_FEE_AMOUNT = "2.50";
    private static final String QUOTE_CYCLE = "P1M";
    private static final String CLASSIFIEDS_PACKAGE_ID = "CP-7";
    private static final String BUY_NOW_FORMAT = "BUY_NOW";
    private static final String AUCTION_FORMAT = "AUCTION";
    private static final String NET_AMOUNT = "81.29";
    private static final String STARTING_AMOUNT = "10.00";
    private static final String MINIMAL_AMOUNT = "50.00";
    private static final String FEE_MARKETPLACE = "allegro-pl";
    private static final String PUBLICATION_DURATION = "P30D";
    private static final String FUNDRAISING_ID = "camp-9";
    private static final String BASE_PACKAGE_ID = "base-1";
    private static final String EXTRA_PACKAGE_ID = "extra-1";
    private static final String PARAM_ID = "11323";
    private static final String PARAM_VALUE = "Red";
    private static final String TRUE_LITERAL = "true";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long TEST_RETRY_AFTER = 1L;
    private static final String TEST_ERROR_CODE = "ValidationError";
    private static final String TEST_ERROR_PATH = "offer.category.id";

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
    // spec-derived: not yet wire-verified (one sale commission + one recurring quote)
    private static final String FEE_PREVIEW_RESPONSE = """
            {"commissions":[{"name":"Sale commission","type":"SALE",
                "fee":{"amount":"2.50","currency":"PLN"}}],
             "quotes":[{"name":"Promo","type":"PROMO",
                "fee":{"amount":"1.00","currency":"PLN"},"cycleDuration":"P1M",
                "classifiedsPackage":{"id":"CP-7"}}]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String VALIDATION_ERROR_RESPONSE = """
            {"errors":[{"code":"ValidationError","message":"Invalid category",
              "userMessage":"Invalid category","path":"offer.category.id","details":null,"metadata":null}]}
            """;
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Not found","userMessage":"Not found","path":null}]}
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
            assertEquals(Money.of(QUOTE_FEE_AMOUNT, TEST_CURRENCY), quote.feeAmount());
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

    @Test
    void feePreview_whenBuyNowRequest_postsOfferBodyAndMapsCommissionsAndQuotes(WireMockRuntimeInfo wmInfo) {
        // given — the body carries the category and a Buy Now selling mode
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(FEE_PREVIEW_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.offer.category.id", equalTo(TEST_CATEGORY_ID)))
                .withRequestBody(matchingJsonPath("$.offer.sellingMode.format", equalTo(BUY_NOW_FORMAT)))
                .withRequestBody(matchingJsonPath("$.offer.sellingMode.price.amount", equalTo(PRICE_AMOUNT)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(FEE_PREVIEW_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            FeePreview preview = allegro.pricing().feePreview(OfferFeePreviewRequest.builder()
                    .categoryId(TEST_CATEGORY_ID)
                    .price(Money.of(PRICE_AMOUNT, TEST_CURRENCY))
                    .build());

            // then — commissions and recurring quotes both map, including the cycle
            assertEquals(1, preview.commissions().size());
            assertEquals(Money.of(COMMISSION_FEE_AMOUNT, TEST_CURRENCY),
                    preview.commissions().get(0).feeAmount());
            assertEquals(1, preview.quotes().size());
            assertEquals(QUOTE_CYCLE, preview.quotes().get(0).cycleDuration());
            assertEquals(CLASSIFIEDS_PACKAGE_ID, preview.quotes().get(0).classifiedsPackageId());
            verify(1, postRequestedFor(urlEqualTo(FEE_PREVIEW_PATH)));
        }
    }

    @Test
    void feePreview_whenOfferIdGiven_includesOfferIdInBody(WireMockRuntimeInfo wmInfo) {
        // given — an existing offer id must reach the request body
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(FEE_PREVIEW_PATH))
                .withRequestBody(matchingJsonPath("$.offer.id", equalTo(FEE_OFFER_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(FEE_PREVIEW_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            FeePreview preview = allegro.pricing().feePreview(OfferFeePreviewRequest.builder()
                    .categoryId(TEST_CATEGORY_ID)
                    .price(Money.of(PRICE_AMOUNT, TEST_CURRENCY))
                    .offerId(FEE_OFFER_ID)
                    .build());

            // then
            assertEquals(1, preview.commissions().size());
            verify(1, postRequestedFor(urlEqualTo(FEE_PREVIEW_PATH)));
        }
    }

    @Test
    void feePreview_whenFullBuyNowBody_mapsEveryFeeAffectingInputToTheWire(WireMockRuntimeInfo wmInfo) {
        // given — every optional fee-affecting input must reach the request body
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(FEE_PREVIEW_PATH))
                .withRequestBody(matchingJsonPath("$.marketplaceId", equalTo(FEE_MARKETPLACE)))
                .withRequestBody(matchingJsonPath("$.offer.sellingMode.format", equalTo(BUY_NOW_FORMAT)))
                .withRequestBody(matchingJsonPath("$.offer.sellingMode.price.amount", equalTo(PRICE_AMOUNT)))
                .withRequestBody(matchingJsonPath("$.offer.sellingMode.netPrice.amount", equalTo(NET_AMOUNT)))
                .withRequestBody(matchingJsonPath("$.offer.fundraisingCampaign.id", equalTo(FUNDRAISING_ID)))
                .withRequestBody(matchingJsonPath("$.offer.publication.duration", equalTo(PUBLICATION_DURATION)))
                .withRequestBody(matchingJsonPath("$.offer.promotion.emphasized10d", equalTo(TRUE_LITERAL)))
                .withRequestBody(matchingJsonPath("$.offer.promotion.departmentPage", equalTo(TRUE_LITERAL)))
                .withRequestBody(matchingJsonPath("$.offer.parameters[0].id", equalTo(PARAM_ID)))
                .withRequestBody(matchingJsonPath("$.offer.parameters[0].values[0]", equalTo(PARAM_VALUE)))
                .withRequestBody(matchingJsonPath(
                        "$.classifiedsPackages.basePackage.id", equalTo(BASE_PACKAGE_ID)))
                .withRequestBody(matchingJsonPath(
                        "$.classifiedsPackages.extraPackages[0].id", equalTo(EXTRA_PACKAGE_ID)))
                .withRequestBody(matchingJsonPath(
                        "$.classifiedsPackages.extraPackages[0].republish", equalTo(TRUE_LITERAL)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(FEE_PREVIEW_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.pricing().feePreview(OfferFeePreviewRequest.builder()
                    .categoryId(TEST_CATEGORY_ID)
                    .sellingMode(FeePreviewSellingMode.buyNow(
                            Money.of(PRICE_AMOUNT, TEST_CURRENCY), Money.of(NET_AMOUNT, TEST_CURRENCY)))
                    .marketplaceId(FEE_MARKETPLACE)
                    .fundraisingCampaignId(FUNDRAISING_ID)
                    .publicationDuration(PUBLICATION_DURATION)
                    .emphasizedForTenDays()
                    .onDepartmentPage()
                    .addParameter(OfferParameter.ofValues(PARAM_ID, List.of(PARAM_VALUE)))
                    .classifiedsPackages(new ClassifiedsPackages(BASE_PACKAGE_ID,
                            List.of(new ClassifiedsExtraPackage(EXTRA_PACKAGE_ID, true))))
                    .build());

            // then — the single mapped POST carried the whole body
            verify(1, postRequestedFor(urlEqualTo(FEE_PREVIEW_PATH)));
        }
    }

    @Test
    void feePreview_whenAuctionSellingMode_mapsStartingAndMinimalPrice(WireMockRuntimeInfo wmInfo) {
        // given — an auction body carries the auction format and both prices
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(FEE_PREVIEW_PATH))
                .withRequestBody(matchingJsonPath("$.offer.sellingMode.format", equalTo(AUCTION_FORMAT)))
                .withRequestBody(matchingJsonPath(
                        "$.offer.sellingMode.startingPrice.amount", equalTo(STARTING_AMOUNT)))
                .withRequestBody(matchingJsonPath(
                        "$.offer.sellingMode.minimalPrice.amount", equalTo(MINIMAL_AMOUNT)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(FEE_PREVIEW_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.pricing().feePreview(OfferFeePreviewRequest.builder()
                    .categoryId(TEST_CATEGORY_ID)
                    .sellingMode(FeePreviewSellingMode.auction(
                            Money.of(STARTING_AMOUNT, TEST_CURRENCY), Money.of(MINIMAL_AMOUNT, TEST_CURRENCY)))
                    .build());

            // then
            verify(1, postRequestedFor(urlEqualTo(FEE_PREVIEW_PATH)));
        }
    }

    @Test
    void feePreview_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(FEE_PREVIEW_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(VALIDATION_ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var request = OfferFeePreviewRequest.builder()
                    .categoryId(TEST_CATEGORY_ID).price(Money.of(PRICE_AMOUNT, TEST_CURRENCY)).build();
            var pricing = allegro.pricing();

            // then — field errors survive; a POST is not retried
            AllegroBadRequestException failure = assertThrows(
                    AllegroBadRequestException.class, () -> pricing.feePreview(request));
            assertEquals(TEST_ERROR_CODE, failure.errors().get(0).code());
            assertEquals(TEST_ERROR_PATH, failure.errors().get(0).path());
            verify(1, postRequestedFor(urlEqualTo(FEE_PREVIEW_PATH)));
        }
    }

    @Test
    void feePreview_when5xx_throwsServerAndDoesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(FEE_PREVIEW_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var request = OfferFeePreviewRequest.builder()
                    .categoryId(TEST_CATEGORY_ID).price(Money.of(PRICE_AMOUNT, TEST_CURRENCY)).build();
            var pricing = allegro.pricing();

            // then — POST is not retried by default
            assertThrows(AllegroServerException.class, () -> pricing.feePreview(request));
            verify(1, postRequestedFor(urlEqualTo(FEE_PREVIEW_PATH)));
        }
    }

    @Test
    void quotes_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(QUOTES_URL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var pricing = allegro.pricing();
            var offerIds = List.of(OFFER_ID_ONE, OFFER_ID_TWO);

            // then
            AllegroNotFoundException failure = assertThrows(
                    AllegroNotFoundException.class, () -> pricing.quotes(offerIds));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
        }
    }

    @Test
    void quotes_when429_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — every attempt is throttled; the policy allows one retry
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(QUOTES_URL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(TEST_RETRY_AFTER))));

        try (AllegroClient allegro = client(wmInfo)) {
            var pricing = allegro.pricing();
            var offerIds = List.of(OFFER_ID_ONE, OFFER_ID_TWO);

            // then — retried once (verify 2), then surfaced with Retry-After
            AllegroRateLimitException failure = assertThrows(
                    AllegroRateLimitException.class, () -> pricing.quotes(offerIds));
            assertEquals(TEST_RETRY_AFTER, failure.retryAfterSeconds());
            verify(2, getRequestedFor(urlEqualTo(QUOTES_URL)));
        }
    }

    @Test
    void quotes_whenOfferIdsEmpty_throwsWithoutCallingServer(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);

        try (AllegroClient allegro = client(wmInfo)) {
            var pricing = allegro.pricing();

            // then — fail-fast on the required, repeated offer.id filter; no request is sent
            assertThrows(IllegalArgumentException.class, () -> pricing.quotes(List.of()));
            verify(0, getRequestedFor(urlEqualTo(QUOTES_URL)));
        }
    }
}
