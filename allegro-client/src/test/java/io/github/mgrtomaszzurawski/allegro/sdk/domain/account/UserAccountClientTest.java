/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SalesQuality;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facade test for the direct {@code client.user()} report endpoints:
 * {@code salesQuality()} and {@code smartClassification()} (with and without a
 * marketplace filter), proving nested mapping and query-parameter handling.
 */
@WireMockTest
class UserAccountClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String SALES_QUALITY_PATH = "/sale/quality";
    private static final String SMART_PATH = "/sale/smart";
    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String SMART_WITH_MARKETPLACE_PATH =
            SMART_PATH + "?marketplaceId=" + MARKETPLACE_ID;
    private static final String METRIC_CODE = "SHIPMENT_TIME";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String SALES_QUALITY_RESPONSE = """
            {"quality":[{"resultFor":"2025-01-15","score":95.5,"maxScore":100,"grade":"A",
              "metrics":[{"code":"%s","name":"Shipment time","score":48,"maxScore":50}]}]}
            """.formatted(METRIC_CODE);
    private static final String SMART_RESPONSE = """
            {"classification":{"fulfilled":true,"lastChanged":"2025-01-15T10:00:00Z"},
             "conditions":[{"code":"%s","name":"Shipment time","description":"ships fast",
               "value":1.5,"threshold":2.0,"fulfilled":true,"required":true}],
             "excludedDeliveryMethods":[{"id":"dm-1"}]}
            """.formatted(METRIC_CODE);

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
    void salesQuality_whenReturned_mapsDaysAndMetrics(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SALES_QUALITY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(SALES_QUALITY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SalesQuality quality = allegro.user().salesQuality();

            // then
            assertEquals(1, quality.days().size());
            SalesQuality.Day day = quality.days().get(0);
            assertEquals("A", day.grade());
            assertEquals(1, day.metrics().size());
            assertEquals(METRIC_CODE, day.metrics().get(0).code());
        }
    }

    @Test
    void smartClassification_whenNoMarketplace_omitsQueryParameter(WireMockRuntimeInfo wmInfo) {
        // given — stub matches ONLY the bare path (no query string)
        stubFor(get(urlEqualTo(SMART_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SMART_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SmartClassification report = allegro.user().smartClassification();

            // then — request carried no marketplaceId, report mapped
            assertTrue(report.fulfilled());
            assertEquals(1, report.conditions().size());
            assertEquals(METRIC_CODE, report.conditions().get(0).code());
            assertEquals(List.of("dm-1"), report.excludedDeliveryMethodIds());
            verify(1, getRequestedFor(urlEqualTo(SMART_PATH)));
        }
    }

    @Test
    void smartClassification_whenMarketplaceGiven_sendsMarketplaceIdQuery(WireMockRuntimeInfo wmInfo) {
        // given — stub matches ONLY the path WITH the marketplaceId query
        stubFor(get(urlEqualTo(SMART_WITH_MARKETPLACE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SMART_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SmartClassification report = allegro.user().smartClassification(MARKETPLACE_ID);

            // then
            assertFalse(report.conditions().isEmpty());
            verify(1, getRequestedFor(urlEqualTo(SMART_WITH_MARKETPLACE_PATH)));
        }
    }
}
