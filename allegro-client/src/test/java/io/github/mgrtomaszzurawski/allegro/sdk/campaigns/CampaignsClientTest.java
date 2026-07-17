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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignRefusalReason;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignSchedule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.CampaignType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.RefusalMessage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SchedulePolicyType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the badge-campaigns starter slice: vendor headers + bearer
 * token on the wire, the marketplace filter as a query parameter, the full
 * {@code *Raw} → domain-record mapping (enums, nested records, schedule dates),
 * and the shared error/re-auth path surfacing for this facade.
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
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String CAMPAIGNS_FIXTURE = "campaigns/badge-campaigns.json";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified — the badge-campaigns response shape is
    // taken from the OpenAPI spec; the bucket-H sandbox exploration pass confirms
    // it (and drops this mark) before the bucket's final PR (TESTING.md §1).
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Not found","userMessage":"Nie znaleziono","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static BadgeCampaign expectedBargainCampaign() {
        return new BadgeCampaign(
                "BARGAIN",
                "Strefa Okazji",
                MARKETPLACE_PL,
                CampaignType.DISCOUNT,
                false,
                List.of(new CampaignRefusalReason("BB5",
                        List.of(new RefusalMessage("Currency is not supported.",
                                "https://na.allegro.pl/regulamin")))),
                new CampaignSchedule(SchedulePolicyType.WITHIN,
                        OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                        OffsetDateTime.parse("2026-08-01T10:00:00Z")),
                new CampaignSchedule(SchedulePolicyType.ALWAYS, null, null),
                new CampaignSchedule(SchedulePolicyType.NEVER, null, null),
                "https://na.allegro.pl/regulamin-kampania-BARGAIN",
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
            assertEquals("BARGAIN", campaigns.get(0).id());
            verify(1, getRequestedFor(urlPathEqualTo(BADGE_CAMPAIGNS_PATH))
                    .withQueryParam(MARKETPLACE_PARAM, equalTo(MARKETPLACE_PL)));
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
            var badges = allegro.campaigns().badges();

            // then — the server's answer surfaces as the typed exception with its trace id
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, badges::availableCampaigns);
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
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
            assertEquals("BARGAIN", campaigns.get(0).id());
            verify(2, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH)));
            verify(1, getRequestedFor(urlEqualTo(BADGE_CAMPAIGNS_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }
}
