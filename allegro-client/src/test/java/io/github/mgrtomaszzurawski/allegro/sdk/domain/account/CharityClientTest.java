/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.CharitySearch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.FundraisingCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facade test for {@code client.charity()} — a beta search that must send the
 * beta vendor Accept header and the required phrase/limit query parameters.
 */
@WireMockTest
class CharityClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String VND_ALLEGRO_BETA_V1 = "application/vnd.allegro.beta.v1+json";
    private static final String CAMPAIGNS_PATH = "/charity/fundraising-campaigns";
    private static final String PHRASE = "children";
    private static final int LIMIT = 20;
    private static final String ORGANIZATION_NAME = "Kids Foundation";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String CAMPAIGNS_RESPONSE = """
            {"campaigns":[{"id":"c1","name":"Help Kids","organization":{"name":"%s"}}]}
            """.formatted(ORGANIZATION_NAME);

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
    void searchCampaigns_whenReturned_sendsBetaAcceptAndPhraseLimitAndMaps(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CAMPAIGNS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<FundraisingCampaign> campaigns = allegro.charity().searchCampaigns(
                    CharitySearch.builder().phrase(PHRASE).limit(LIMIT).build());

            // then — organization flattened to its name
            assertEquals(1, campaigns.size());
            assertEquals("c1", campaigns.get(0).id());
            assertEquals(ORGANIZATION_NAME, campaigns.get(0).organizationName());
            // and — the beta Accept header + required query params went on the wire
            verify(getRequestedFor(urlPathEqualTo(CAMPAIGNS_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(VND_ALLEGRO_BETA_V1))
                    .withQueryParam("phrase", equalTo(PHRASE))
                    .withQueryParam("limit", equalTo(String.valueOf(LIMIT))));
        }
    }
}
