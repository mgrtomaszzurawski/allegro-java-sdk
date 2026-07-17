/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.campaigns;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignRefusalReason;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignSchedule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.RefusalMessage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SchedulePolicyType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
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
}
