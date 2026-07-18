/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of {@code offers().rating(offerId)} — the offer's aggregated
 * buyer rating. Pins the mapping (average, totals, score/size buckets), the
 * unrated-offer shape, and fail-fast on a null offer id. The transport error
 * table lives on the bucket-A {@code Offers} facade tests.
 */
@WireMockTest
class OfferRatingClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_OFFER_ID = "8235476198";
    private static final String RATING_PATH = "/sale/offers/" + TEST_OFFER_ID + "/rating";
    private static final String TEST_AVERAGE = "4.53";
    private static final int TEST_TOTAL = 42;
    private static final int TOP_SCORE_COUNT = 30;
    private static final int SIZE_COUNT = 20;
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified.
    private static final String RATING_RESPONSE = """
            {"averageScore":"%s","totalResponses":%d,
             "scoreDistribution":[{"name":"5","count":%d},{"name":"4","count":8}],
             "sizeFeedback":[{"name":"TRUE_TO_SIZE","count":%d}]}
            """.formatted(TEST_AVERAGE, TEST_TOTAL, TOP_SCORE_COUNT, SIZE_COUNT);
    private static final String UNRATED_RESPONSE = """
            {"averageScore":null,"totalResponses":0,"scoreDistribution":[],"sizeFeedback":[]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static void stubToken() {
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    @Test
    void rating_whenOfferHasRatings_mapsAverageTotalsAndBuckets(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(RATING_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(RATING_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferRating rating = allegro.offers().rating(TEST_OFFER_ID);

            // then
            assertEquals(TEST_AVERAGE, rating.averageScore());
            assertEquals(TEST_TOTAL, rating.totalResponses());
            assertEquals(2, rating.scoreDistribution().size());
            assertEquals("5", rating.scoreDistribution().get(0).name());
            assertEquals(TOP_SCORE_COUNT, rating.scoreDistribution().get(0).count());
            assertEquals("TRUE_TO_SIZE", rating.sizeFeedback().get(0).name());
            assertEquals(SIZE_COUNT, rating.sizeFeedback().get(0).count());
            verify(1, getRequestedFor(urlPathEqualTo(RATING_PATH)));
        }
    }

    @Test
    void rating_whenOfferHasNoRatings_mapsNullAverageAndEmptyBuckets(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(RATING_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(UNRATED_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferRating rating = allegro.offers().rating(TEST_OFFER_ID);

            // then — an unrated offer maps to a null average and empty breakdowns
            assertNull(rating.averageScore());
            assertEquals(0, rating.totalResponses());
            assertTrue(rating.scoreDistribution().isEmpty());
            assertTrue(rating.sizeFeedback().isEmpty());
        }
    }

    @Test
    void rating_whenOfferIdNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var offers = allegro.offers();

            // then — the offer id is required, fail-fast before the wire
            assertThrows(NullPointerException.class, () -> offers.rating(null));
            verify(0, getRequestedFor(urlPathEqualTo(RATING_PATH)));
        }
    }
}
