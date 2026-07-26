/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
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
import static org.junit.jupiter.api.Assertions.assertNull;

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
import java.time.OffsetDateTime;
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

    private static final String CONVERSIONS_PATH = "/affiliate/conversions/cps";
    private static final String PUBLISHER_AMOUNT = "1.00";
    private static final String CURRENCY_PLN = "PLN";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String OFFER_CATEGORY_ID = "257";
    private static final String UTM_SOURCE_KEY = "utm_source";
    private static final String UTM_SOURCE_VALUE = "blog";
    private static final String CLICK_ID_KEY = "clickId";
    private static final String CLICK_ID_VALUE = "abc123";
    private static final String CONVERSIONS_RESPONSE = """
            {"conversions":[{"id":"conv-1","status":"CONFIRMED","quantity":2,
              "lastModifiedAt":"2025-01-10T12:00:00Z","orderCreatedAt":"2025-01-09T12:00:00Z",
              "marketplace":{"id":"allegro-pl"},
              "offer":{"id":"o1","name":"Widget","category":{"id":"%s"},
                "unitPrice":{"amount":"10.00","currency":"PLN"},
                "seller":{"login":"seller1"}},
              "commission":{"publisher":{"amount":"%s","currency":"%s"},
                "allegro":{"amount":"0.50","currency":"%s"}},
              "publisherUrlParameters":{"%s":"%s","%s":"%s"}}]}
            """.formatted(OFFER_CATEGORY_ID, PUBLISHER_AMOUNT, CURRENCY_PLN, CURRENCY_PLN,
            UTM_SOURCE_KEY, UTM_SOURCE_VALUE, CLICK_ID_KEY, CLICK_ID_VALUE);
    // A conversion whose price/commission objects are present but omit their
    // amount/currency — must map to null money, not abort the whole stream.
    private static final String INCOMPLETE_PRICE_RESPONSE = """
            {"conversions":[{"id":"conv-2","status":"CREATED",
              "offer":{"id":"o2","unitPrice":{"currency":"PLN"}},
              "commission":{"publisher":{}}}]}
            """;
    // A status value this SDK release does not model — Layer-1 degrades it to
    // the generated sentinel (C3), the domain enum lands on UNKNOWN, and the
    // whole response must still deserialize.
    private static final String UNKNOWN_STATUS_RESPONSE = """
            {"conversions":[{"id":"conv-3","status":"SETTLED_LATER","quantity":1}]}
            """;
    private static final String REJECTED_STATUS_RESPONSE = """
            {"conversions":[{"id":"conv-4","status":"REJECTED","quantity":1}]}
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
            assertEquals("conv-1", conversion.id());
            assertEquals(ConversionStatus.CONFIRMED, conversion.status());
            assertEquals(2, conversion.quantity().intValue());
            assertEquals(OffsetDateTime.parse("2025-01-10T12:00:00Z"), conversion.lastModifiedAt());
            assertEquals(OffsetDateTime.parse("2025-01-09T12:00:00Z"), conversion.orderCreatedAt());
            assertEquals("allegro-pl", conversion.marketplaceId());
            assertEquals("o1", conversion.offer().id());
            assertEquals("Widget", conversion.offer().name());
            assertEquals(Money.of("10.00", CURRENCY_PLN), conversion.offer().unitPrice());
            assertEquals(Money.of(PUBLISHER_AMOUNT, CURRENCY_PLN), conversion.commission().publisher());
            assertEquals(Money.of("0.50", CURRENCY_PLN), conversion.commission().allegro());
            assertEquals("seller1", conversion.offer().sellerLogin());
            assertEquals(OFFER_CATEGORY_ID, conversion.offer().categoryId());
            // and — the affiliate tracking parameters are echoed back in full
            assertEquals(UTM_SOURCE_VALUE, conversion.publisherUrlParameters().get(UTM_SOURCE_KEY));
            assertEquals(CLICK_ID_VALUE, conversion.publisherUrlParameters().get(CLICK_ID_KEY));
            // and — beta Accept header + status filter + first-page offset on the wire
            verify(getRequestedFor(urlPathEqualTo(CONVERSIONS_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                    .withQueryParam("status", equalTo("CONFIRMED"))
                    .withQueryParam("offset", equalTo("0")));
            // and — exactly one page fetched (short page ended iteration)
            verify(1, getRequestedFor(urlPathEqualTo(CONVERSIONS_PATH)));
        }
    }

    @Test
    void streamCpsConversions_whenPriceAmountMissing_mapsMoneyToNullWithoutFailing(
            WireMockRuntimeInfo wmInfo) {
        // given — offer.unitPrice and commission.publisher lack amount/currency
        stubFor(get(urlPathEqualTo(CONVERSIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(INCOMPLETE_PRICE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<CpsConversion> conversions =
                    allegro.affiliate().streamCpsConversions(ConversionFilter.all()).toList();

            // then — the incomplete price maps to null, the conversion still yields
            assertEquals(1, conversions.size());
            assertEquals(ConversionStatus.CREATED, conversions.get(0).status());
            assertNull(conversions.get(0).offer().unitPrice());
            assertNull(conversions.get(0).commission().publisher());
        }
    }

    @Test
    void streamCpsConversions_whenStatusRejected_mapsRejectedEnum(WireMockRuntimeInfo wmInfo) {
        // given — a known REJECTED status (the remaining modelled branch)
        stubFor(get(urlPathEqualTo(CONVERSIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(REJECTED_STATUS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<CpsConversion> conversions =
                    allegro.affiliate().streamCpsConversions(ConversionFilter.all()).toList();

            // then — the REJECTED wire value maps to the modelled enum, not UNKNOWN
            assertEquals(1, conversions.size());
            assertEquals(ConversionStatus.REJECTED, conversions.get(0).status());
        }
    }

    @Test
    void streamCpsConversions_whenStatusFilterUnknown_omitsStatusQueryParameter(
            WireMockRuntimeInfo wmInfo) {
        // given — the forward-compat UNKNOWN sentinel is not a valid request value
        stubFor(get(urlPathEqualTo(CONVERSIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CONVERSIONS_RESPONSE)));
        ConversionFilter filter = ConversionFilter.builder()
                .status(ConversionStatus.UNKNOWN)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.affiliate().streamCpsConversions(filter).toList();

            // then — status is dropped, not sent verbatim (which the server would reject)
            verify(getRequestedFor(urlPathEqualTo(CONVERSIONS_PATH))
                    .withQueryParam("status", absent()));
        }
    }

    @Test
    void streamCpsConversions_whenUnknownWireStatus_mapsToUnknownWithoutFailing(
            WireMockRuntimeInfo wmInfo) {
        // given — a status value newer than this SDK release models
        stubFor(get(urlPathEqualTo(CONVERSIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_STATUS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<CpsConversion> conversions =
                    allegro.affiliate().streamCpsConversions(ConversionFilter.all()).toList();

            // then — degraded to UNKNOWN end-to-end, the response still deserialized
            assertEquals(1, conversions.size());
            assertEquals(ConversionStatus.UNKNOWN, conversions.get(0).status());
        }
    }
}
