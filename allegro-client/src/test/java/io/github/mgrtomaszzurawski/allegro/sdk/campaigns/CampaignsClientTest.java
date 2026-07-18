/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.campaigns;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
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
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgePatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.Badge;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplicationStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperationStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgePrices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignRefusalReason;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignSchedule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.RefusalMessage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SchedulePolicyType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the badge-campaigns starter slice: vendor headers + bearer
 * token on the wire, the marketplace filter as a query parameter, the full
 * {@code *Raw} → domain-record mapping (enums, nested records, schedule dates),
 * fail-fast validation of the marketplace overload, and the mandatory error
 * table (TESTING.md §1) against {@code GET /sale/badge-campaigns}.
 */
@WireMockTest
class CampaignsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String BADGE_CAMPAIGNS_PATH = "/sale/badge-campaigns";
    private static final String MARKETPLACE_PARAM = "marketplace.id";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String BLANK_MARKETPLACE = "   ";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String CAMPAIGNS_FIXTURE = "campaigns/badge-campaigns.json";

    // Expected mapped values — kept as constants so the fixture and the expected
    // record can never silently drift apart (TESTING.md §1).
    private static final String TEST_CAMPAIGN_ID = "BARGAIN";
    private static final String TEST_CAMPAIGN_NAME = "Strefa Okazji";
    private static final CampaignType TEST_CAMPAIGN_TYPE = CampaignType.DISCOUNT;
    private static final String TEST_REFUSAL_CODE = "BB5";
    private static final String TEST_REFUSAL_MESSAGE = "Currency is not supported.";
    private static final String TEST_REFUSAL_LINK = "https://na.allegro.pl/regulamin";
    private static final String TEST_REGULATIONS_LINK = "https://na.allegro.pl/regulamin-kampania-BARGAIN";
    private static final String TEST_APPLICATION_FROM = "2026-07-01T10:00:00Z";
    private static final String TEST_APPLICATION_TO = "2026-08-01T10:00:00Z";

    private static final String TEST_BAD_REQUEST_CODE = "VALIDATION_ERROR";
    private static final String TEST_BAD_REQUEST_PATH = "marketplace.id";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String SCENARIO_RECOVER = "recover-5xx";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String STATE_RECOVERED = "recovered";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final String RETRY_AFTER_SHORT_VALUE = "1";
    private static final long RETRY_AFTER_SHORT_SECONDS = 1L;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified — the errors[] shape is taken from the
    // spec; a live 400/404 capture during the bucket-H exploration pass confirms
    // it (and drops these marks) before the bucket's final PR (TESTING.md §1).
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Not found","userMessage":"Nie znaleziono","path":null}]}
            """;
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"%s","message":"Invalid marketplace identifier",
              "userMessage":"Nieprawidłowy marketplace","path":"%s","details":null}]}
            """.formatted(TEST_BAD_REQUEST_CODE, TEST_BAD_REQUEST_PATH);

    // ---- badges PR-2: apply / streams / application / update ----
    private static final int PAGE_SIZE = 100;
    private static final int HTTP_ACCEPTED = 202;
    private static final String BADGES_PATH = "/sale/badges";
    private static final String BADGE_APPLICATIONS_PATH = "/sale/badge-applications";
    private static final String BADGE_OPERATIONS_PATH = "/sale/badge-operations";
    private static final String TEST_OFFER_ID = "12345678";
    private static final String TEST_APPLICATION_ID = "app-1";
    private static final String TEST_OPERATION_ID = "op-1";
    private static final String BADGE_UPDATE_PATH =
            BADGES_PATH + "/offers/" + TEST_OFFER_ID + "/campaigns/" + TEST_CAMPAIGN_ID;
    private static final String APPLICATION_FIXTURE = "campaigns/badge-application.json";
    private static final String BADGES_FIXTURE = "campaigns/badges-list.json";

    private static final String OFFER_PARAM = "offer.id";
    private static final String CAMPAIGN_PARAM = "campaign.id";
    private static final String OFFSET_PARAM = "offset";

    // Expected mapped values for the application/badge/operation fixtures.
    private static final String TEST_CURRENCY_PLN = "PLN";
    private static final String TEST_BARGAIN_AMOUNT = "29.99";
    private static final String TEST_MARKET_AMOUNT = "39.99";
    private static final String TEST_SUBSIDY_TARGET_AMOUNT = "27.99";
    private static final String TEST_SUBSIDY_SELLER_AMOUNT = "28.99";
    private static final String TEST_NEW_BARGAIN_AMOUNT = "19.99";
    private static final BigDecimal TEST_APP_STOCK = new BigDecimal("10");
    private static final BigDecimal TEST_BADGE_STOCK = new BigDecimal("5");
    private static final int TEST_LIMIT_PER_USER = 3;
    private static final String TEST_APP_CREATED = "2026-07-15T09:00:00Z";
    private static final String TEST_APP_UPDATED = "2026-07-15T09:05:00Z";
    private static final String TEST_OP_CREATED = "2026-07-16T10:00:00Z";
    private static final String TEST_OP_UPDATED = "2026-07-16T10:00:05Z";
    private static final String TEST_OP_REJECT_CODE = "PRICE_TOO_HIGH";
    private static final String TEST_OP_REJECT_MESSAGE = "The proposed price is above the allowed maximum.";

    // JSON-path expressions and wire status literals for request-body matchers.
    private static final String JSON_CAMPAIGN_ID = "$.campaign.id";
    private static final String JSON_OFFER_ID = "$.offer.id";
    private static final String JSON_BARGAIN_AMOUNT = "$.prices.bargain.amount";
    private static final String JSON_PROCESS_STATUS = "$.process.status";
    private static final String JSON_PRICE_VALUE_AMOUNT = "$.prices.bargain.value.amount";
    private static final String WIRE_STATUS_FINISHED = "FINISHED";

    private static final String SCENARIO_POLL = "poll-operation";
    private static final String STATE_TERMINAL = "terminal";

    private static final String PATCH_ACCEPTED_RESPONSE = """
            {"id":"%s"}
            """.formatted(TEST_OPERATION_ID);
    private static final String OPERATION_TEMPLATE = """
            {"id":"%s","type":"UPDATE","createdAt":"%s","updatedAt":"%s",
             "campaign":{"id":"%s"},"offer":{"id":"%s"},
             "process":{"status":"%s","rejectionReasons":%s}}
            """;
    private static final String REASONS_EMPTY = "[]";
    private static final String REASONS_DECLINED = """
            [{"code":"%s","messages":[{"text":"%s","link":null}]}]
            """.formatted(TEST_OP_REJECT_CODE, TEST_OP_REJECT_MESSAGE);

    private static String operationResponse(BadgeOperationStatus status, String rejectionReasons) {
        return OPERATION_TEMPLATE.formatted(TEST_OPERATION_ID, TEST_OP_CREATED, TEST_OP_UPDATED,
                TEST_CAMPAIGN_ID, TEST_OFFER_ID, status.name(), rejectionReasons);
    }

    private static String fullPageOfApplications(int count) {
        StringBuilder json = new StringBuilder("{\"badgeApplications\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"app").append(index)
                    .append("\",\"createdAt\":\"").append(TEST_APP_CREATED)
                    .append("\",\"updatedAt\":\"").append(TEST_APP_UPDATED)
                    .append("\",\"campaign\":{\"id\":\"").append(TEST_CAMPAIGN_ID)
                    .append("\"},\"offer\":{\"id\":\"").append(TEST_OFFER_ID)
                    .append("\"},\"process\":{\"status\":\"REQUESTED\",\"rejectionReasons\":[]}}");
        }
        return json.append("]}").toString();
    }

    private static BadgeApplication expectedApplication() {
        return new BadgeApplication(
                TEST_APPLICATION_ID,
                OffsetDateTime.parse(TEST_APP_CREATED),
                OffsetDateTime.parse(TEST_APP_UPDATED),
                TEST_CAMPAIGN_ID,
                TEST_OFFER_ID,
                BadgeApplicationStatus.REQUESTED,
                List.of(),
                Money.of(TEST_BARGAIN_AMOUNT, TEST_CURRENCY_PLN),
                TEST_APP_STOCK,
                TEST_LIMIT_PER_USER);
    }

    private static Badge expectedBadge() {
        return new Badge(
                TEST_OFFER_ID,
                TEST_CAMPAIGN_ID,
                TEST_CAMPAIGN_NAME,
                BadgeStatus.ACTIVE,
                List.of(),
                new CampaignSchedule(SchedulePolicyType.WITHIN,
                        OffsetDateTime.parse(TEST_APPLICATION_FROM),
                        OffsetDateTime.parse(TEST_APPLICATION_TO)),
                new BadgePrices(
                        Money.of(TEST_MARKET_AMOUNT, TEST_CURRENCY_PLN),
                        Money.of(TEST_BARGAIN_AMOUNT, TEST_CURRENCY_PLN),
                        Money.of(TEST_SUBSIDY_TARGET_AMOUNT, TEST_CURRENCY_PLN),
                        Money.of(TEST_SUBSIDY_SELLER_AMOUNT, TEST_CURRENCY_PLN)),
                TEST_BADGE_STOCK);
    }

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

    private static BadgeCampaign expectedBargainCampaign() {
        return new BadgeCampaign(
                TEST_CAMPAIGN_ID,
                TEST_CAMPAIGN_NAME,
                MARKETPLACE_PL,
                TEST_CAMPAIGN_TYPE,
                false,
                List.of(new CampaignRefusalReason(TEST_REFUSAL_CODE,
                        List.of(new RefusalMessage(TEST_REFUSAL_MESSAGE, TEST_REFUSAL_LINK)))),
                new CampaignSchedule(SchedulePolicyType.WITHIN,
                        OffsetDateTime.parse(TEST_APPLICATION_FROM),
                        OffsetDateTime.parse(TEST_APPLICATION_TO)),
                new CampaignSchedule(SchedulePolicyType.ALWAYS, null, null),
                new CampaignSchedule(SchedulePolicyType.NEVER, null, null),
                TEST_REGULATIONS_LINK,
                false);
    }

    @Test
    void availableCampaigns_whenAuthenticated_sendsVendorHeadersAndMapsCampaigns(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BadgeCampaign> campaigns = allegro.campaigns().badges().availableCampaigns();

            // then — the whole Raw graph maps to the expected immutable record
            assertEquals(1, campaigns.size());
            assertEquals(expectedBargainCampaign(), campaigns.get(0));
            verify(1, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
        }
    }

    @Test
    void availableCampaigns_whenMarketplaceGiven_addsMarketplaceQueryParam(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(BADGE_CAMPAIGNS_PATH))
                .withQueryParam(MARKETPLACE_PARAM, equalTo(MARKETPLACE_PL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BadgeCampaign> campaigns =
                    allegro.campaigns().badges().availableCampaigns(MARKETPLACE_PL);

            // then — the filter travelled as an encoded query parameter
            assertEquals(TEST_CAMPAIGN_ID, campaigns.get(0).id());
            verify(1, getRequestedFor(urlPathEqualTo(BADGE_CAMPAIGNS_PATH))
                    .withQueryParam(MARKETPLACE_PARAM, equalTo(MARKETPLACE_PL)));
        }
    }

    @Test
    void availableCampaigns_whenMarketplaceBlank_throwsIllegalArgumentBeforeAnyRequest(
            WireMockRuntimeInfo wmInfo) {
        // given — no HTTP stub: the guard must reject before any call is made
        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();

            // then — fail-fast; the no-arg overload is the way to list all marketplaces
            assertThrows(IllegalArgumentException.class,
                    () -> badges.availableCampaigns(BLANK_MARKETPLACE));
            verify(0, getRequestedFor(urlPathEqualTo(BADGE_CAMPAIGNS_PATH)));
        }
    }

    @Test
    void availableCampaigns_when400WithFieldErrors_throwsBadRequestWithParsedErrors(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();

            // then — the errors[] payload is parsed into typed field errors
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, badges::availableCampaigns);
            assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, failure.statusCode());
            assertEquals(1, failure.errors().size());
            AllegroFieldError fieldError = failure.errors().get(0);
            assertEquals(TEST_BAD_REQUEST_CODE, fieldError.code());
            assertEquals(TEST_BAD_REQUEST_PATH, fieldError.path());
        }
    }

    @Test
    void availableCampaigns_when401Once_reauthenticatesAndReplaysWithFreshToken(
            WireMockRuntimeInfo wmInfo) {
        // given — first call 401, then 200 after re-auth; the token endpoint hands
        // out token-one first and token-two on the second acquisition.
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BadgeCampaign> campaigns = allegro.campaigns().badges().availableCampaigns();

            // then — replayed exactly once, the second request carried the fresh token
            assertEquals(TEST_CAMPAIGN_ID, campaigns.get(0).id());
            verify(2, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
            verify(1, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void availableCampaigns_when401Twice_throwsAuthExceptionAfterSingleReplay(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();

            // then — exactly one replay, then the typed failure with traceId
            AllegroAuthException failure =
                    assertThrows(AllegroAuthException.class, badges::availableCampaigns);
            assertEquals(TestHttpConstants.HTTP_UNAUTHORIZED, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(2, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
        }
    }

    @Test
    void availableCampaigns_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();

            // then — the server's answer surfaces as the typed exception, not retried
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, badges::availableCampaigns);
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
        }
    }

    @Test
    void availableCampaigns_when429Persists_retriesThenThrowsRateLimitWithRetryAfter(
            WireMockRuntimeInfo wmInfo) {
        // given — persistent 429 with a short Retry-After and retries enabled
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SHORT_VALUE)));
        RetryPolicy retryTwice = RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).build();

        try (AllegroClient allegro = client(wmInfo, retryTwice)) {
            Badges badges = allegro.campaigns().badges();

            // then — retried up to the attempt cap, then the typed rate-limit failure
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, badges::availableCampaigns);
            assertEquals(RETRY_AFTER_SHORT_SECONDS, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
        }
    }

    @Test
    void availableCampaigns_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first GET 500, second GET 200 (GETs are retried by default)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BadgeCampaign> campaigns = allegro.campaigns().badges().availableCampaigns();

            // then — the retry recovered the call
            assertEquals(TEST_CAMPAIGN_ID, campaigns.get(0).id());
            verify(2, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
        }
    }

    @Test
    void apply_whenValidRequest_postsApplicationBodyAndDoesNotPoll(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(BADGES_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBodyFile(APPLICATION_FIXTURE)));
        BadgeApplicationRequest request = BadgeApplicationRequest.builder()
                .campaignId(TEST_CAMPAIGN_ID)
                .offerId(TEST_OFFER_ID)
                .bargainPrice(Money.of(TEST_BARGAIN_AMOUNT, TEST_CURRENCY_PLN))
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            BadgeApplication application = allegro.campaigns().badges().apply(request);

            // then — the request body carried campaign, offer and bargain price
            assertEquals(expectedApplication(), application);
            verify(1, postRequestedFor(urlEqualTo(BADGES_PATH))
                    .withRequestBody(matchingJsonPath(JSON_CAMPAIGN_ID, equalTo(TEST_CAMPAIGN_ID)))
                    .withRequestBody(matchingJsonPath(JSON_OFFER_ID, equalTo(TEST_OFFER_ID)))
                    .withRequestBody(matchingJsonPath(JSON_BARGAIN_AMOUNT, equalTo(TEST_BARGAIN_AMOUNT))));
            // D1: an e-mail-verified application is returned as-is, never polled to terminal
            verify(0, getRequestedFor(urlPathEqualTo(BADGE_OPERATIONS_PATH)));
        }
    }

    @Test
    void streamApplications_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(BADGE_APPLICATIONS_PATH))
                .withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfApplications(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — only the first element is consumed
            List<BadgeApplication> firstOnly = allegro.campaigns().badges()
                    .streamApplications(BadgeApplicationFilter.all())
                    .limit(1)
                    .toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(BADGE_APPLICATIONS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo("0")));
            verify(0, getRequestedFor(urlPathEqualTo(BADGE_APPLICATIONS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamApplications_whenFilterGiven_sendsCampaignAndOfferQueryParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(BADGE_APPLICATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfApplications(1))));
        BadgeApplicationFilter filter = BadgeApplicationFilter.builder()
                .campaignId(TEST_CAMPAIGN_ID)
                .offerId(TEST_OFFER_ID)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.campaigns().badges().streamApplications(filter).toList();

            // then — both filters travelled as encoded query parameters
            verify(getRequestedFor(urlPathEqualTo(BADGE_APPLICATIONS_PATH))
                    .withQueryParam(CAMPAIGN_PARAM, equalTo(TEST_CAMPAIGN_ID))
                    .withQueryParam(OFFER_PARAM, equalTo(TEST_OFFER_ID)));
        }
    }

    @Test
    void application_whenFound_mapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(BADGE_APPLICATIONS_PATH + "/" + TEST_APPLICATION_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(APPLICATION_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            BadgeApplication application =
                    allegro.campaigns().badges().application(TEST_APPLICATION_ID);

            // then
            assertEquals(expectedApplication(), application);
            verify(1, getRequestedFor(urlEqualTo(BADGE_APPLICATIONS_PATH + "/" + TEST_APPLICATION_ID)));
        }
    }

    @Test
    void application_whenIdBlank_throwsIllegalArgumentBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        // given — no stub: the guard must reject before any call
        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();

            // then
            assertThrows(IllegalArgumentException.class, () -> badges.application(BLANK_MARKETPLACE));
            verify(0, getRequestedFor(urlPathEqualTo(BADGE_APPLICATIONS_PATH)));
        }
    }

    @Test
    void streamBadges_whenConsumed_mapsBadgeAndSendsMarketplaceFilter(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(BADGES_PATH))
                .withQueryParam(MARKETPLACE_PARAM, equalTo(MARKETPLACE_PL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(BADGES_FIXTURE)));
        BadgeFilter filter = BadgeFilter.builder().marketplaceId(MARKETPLACE_PL).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Badge> badges = allegro.campaigns().badges().streamBadges(filter).toList();

            // then — the badge graph (prices, publication window, status) maps in full
            assertEquals(1, badges.size());
            assertEquals(expectedBadge(), badges.get(0));
            verify(getRequestedFor(urlPathEqualTo(BADGES_PATH))
                    .withQueryParam(MARKETPLACE_PARAM, equalTo(MARKETPLACE_PL)));
        }
    }

    @Test
    void update_whenFinishAndOperationProcessed_patchesThenPollsToTerminal(WireMockRuntimeInfo wmInfo) {
        // given — PATCH accepted (202 {id}), the operation is PROCESSED on first poll
        stubToken(TEST_TOKEN);
        stubFor(patch(urlEqualTo(BADGE_UPDATE_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(PATCH_ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationResponse(BadgeOperationStatus.PROCESSED, REASONS_EMPTY))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            BadgeOperation operation = allegro.campaigns().badges()
                    .update(TEST_OFFER_ID, TEST_CAMPAIGN_ID, BadgePatch.finish());

            // then — the finish body was sent and the operation polled once to terminal
            assertEquals(BadgeOperationStatus.PROCESSED, operation.status());
            assertEquals(BadgeOperationType.UPDATE, operation.type());
            assertEquals(TEST_OPERATION_ID, operation.id());
            verify(1, patchRequestedFor(urlEqualTo(BADGE_UPDATE_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PROCESS_STATUS, equalTo(WIRE_STATUS_FINISHED))));
            verify(1, getRequestedFor(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID)));
        }
    }

    @Test
    void update_whenChangeBargainPrice_sendsPriceValueInPatchBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(patch(urlEqualTo(BADGE_UPDATE_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(PATCH_ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationResponse(BadgeOperationStatus.PROCESSED, REASONS_EMPTY))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.campaigns().badges().update(TEST_OFFER_ID, TEST_CAMPAIGN_ID,
                    BadgePatch.changeBargainPrice(Money.of(TEST_NEW_BARGAIN_AMOUNT, TEST_CURRENCY_PLN)));

            // then — the new bargain price travelled inside the prices patch shape
            verify(1, patchRequestedFor(urlEqualTo(BADGE_UPDATE_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PRICE_VALUE_AMOUNT, equalTo(TEST_NEW_BARGAIN_AMOUNT))));
        }
    }

    @Test
    void update_whenOperationRequestedThenProcessed_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — first poll REQUESTED, second poll PROCESSED
        stubToken(TEST_TOKEN);
        stubFor(patch(urlEqualTo(BADGE_UPDATE_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(PATCH_ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationResponse(BadgeOperationStatus.REQUESTED, REASONS_EMPTY)))
                .willSetStateTo(STATE_TERMINAL));
        stubFor(get(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(STATE_TERMINAL)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationResponse(BadgeOperationStatus.PROCESSED, REASONS_EMPTY))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            BadgeOperation operation = allegro.campaigns().badges()
                    .update(TEST_OFFER_ID, TEST_CAMPAIGN_ID, BadgePatch.finish());

            // then — polled twice, until the operation left REQUESTED
            assertEquals(BadgeOperationStatus.PROCESSED, operation.status());
            verify(2, getRequestedFor(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID)));
        }
    }

    @Test
    void update_whenOperationDeclined_returnsTerminalWithRejectionReasons(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(patch(urlEqualTo(BADGE_UPDATE_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(PATCH_ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationResponse(BadgeOperationStatus.DECLINED, REASONS_DECLINED))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            BadgeOperation operation = allegro.campaigns().badges()
                    .update(TEST_OFFER_ID, TEST_CAMPAIGN_ID, BadgePatch.finish());

            // then — DECLINED is terminal; the rejection reason maps through
            assertEquals(BadgeOperationStatus.DECLINED, operation.status());
            assertEquals(1, operation.rejectionReasons().size());
            assertEquals(TEST_OP_REJECT_CODE, operation.rejectionReasons().get(0).code());
        }
    }

    @Test
    void update_whenOperationNeverTerminalWithinTimeout_throwsAsyncTimeout(WireMockRuntimeInfo wmInfo) {
        // given — the operation stays REQUESTED and the caller passes a zero budget
        stubToken(TEST_TOKEN);
        stubFor(patch(urlEqualTo(BADGE_UPDATE_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(PATCH_ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(BADGE_OPERATIONS_PATH + "/" + TEST_OPERATION_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(operationResponse(BadgeOperationStatus.REQUESTED, REASONS_EMPTY))));

        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();
            BadgePatch finish = BadgePatch.finish();

            // then — the wait budget is exhausted and the typed timeout is thrown
            assertThrows(AllegroAsyncTimeoutException.class,
                    () -> badges.update(TEST_OFFER_ID, TEST_CAMPAIGN_ID, finish, Duration.ZERO));
        }
    }

    @Test
    void update_whenArgsInvalid_throwsIllegalArgumentBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        // given — no stub: each guard must reject before any call
        try (AllegroClient allegro = client(wmInfo)) {
            Badges badges = allegro.campaigns().badges();
            BadgePatch finish = BadgePatch.finish();

            // then — blank offer, blank campaign, and null patch all fail fast
            assertThrows(IllegalArgumentException.class,
                    () -> badges.update(BLANK_MARKETPLACE, TEST_CAMPAIGN_ID, finish));
            assertThrows(IllegalArgumentException.class,
                    () -> badges.update(TEST_OFFER_ID, BLANK_MARKETPLACE, finish));
            assertThrows(IllegalArgumentException.class,
                    () -> badges.update(TEST_OFFER_ID, TEST_CAMPAIGN_ID, null));
            verify(0, patchRequestedFor(urlEqualTo(BADGE_UPDATE_PATH)));
        }
    }
}
