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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.ConversionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.ConversionStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CpsConversion;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facade test for {@code client.affiliate()} — a beta, offset/limit-paginated
 * stream that must send the beta Accept header and filter query parameters and
 * terminate on a short page.
 */
@WireMockTest
class AffiliateClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String VND_ALLEGRO_BETA_V1 = "application/vnd.allegro.beta.v1+json";
    private static final String CONVERSIONS_PATH = "/affiliate/conversions/cps";
    private static final String PUBLISHER_AMOUNT = "1.00";
    private static final String CURRENCY_PLN = "PLN";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String CONVERSIONS_RESPONSE = """
            {"conversions":[{"id":"conv-1","status":"CONFIRMED","quantity":2,
              "marketplace":{"id":"allegro-pl"},
              "offer":{"id":"o1","name":"Widget","unitPrice":{"amount":"10.00","currency":"PLN"},
                "seller":{"login":"seller1"}},
              "commission":{"publisher":{"amount":"%s","currency":"%s"},
                "allegro":{"amount":"0.50","currency":"%s"}}}]}
            """.formatted(PUBLISHER_AMOUNT, CURRENCY_PLN, CURRENCY_PLN);

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
    void streamCpsConversions_whenShortPage_sendsBetaAcceptAndStatusFilterAndMapsMoney(
            WireMockRuntimeInfo wmInfo) {
        // given — one short page (< page size) terminates the stream after one request
        stubFor(get(urlPathEqualTo(CONVERSIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CONVERSIONS_RESPONSE)));
        ConversionFilter filter = ConversionFilter.builder()
                .status(ConversionStatus.CONFIRMED)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<CpsConversion> conversions =
                    allegro.affiliate().streamCpsConversions(filter).toList();

            // then — mapped incl. status enum, nested Money, seller
            assertEquals(1, conversions.size());
            CpsConversion conversion = conversions.get(0);
            assertEquals(ConversionStatus.CONFIRMED, conversion.status());
            assertEquals("allegro-pl", conversion.marketplaceId());
            assertEquals(Money.of("10.00", CURRENCY_PLN), conversion.offer().unitPrice());
            assertEquals(Money.of(PUBLISHER_AMOUNT, CURRENCY_PLN), conversion.commission().publisher());
            assertEquals("seller1", conversion.offer().sellerLogin());
            // and — beta Accept header + status filter + first-page offset on the wire
            verify(getRequestedFor(urlPathEqualTo(CONVERSIONS_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(VND_ALLEGRO_BETA_V1))
                    .withQueryParam("status", equalTo("CONFIRMED"))
                    .withQueryParam("offset", equalTo("0")));
            // and — exactly one page fetched (short page ended iteration)
            verify(1, getRequestedFor(urlPathEqualTo(CONVERSIONS_PATH)));
        }
    }
}
