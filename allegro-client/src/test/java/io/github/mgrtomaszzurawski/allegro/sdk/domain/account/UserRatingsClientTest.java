/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingAnswer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingRemoval;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Answer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Removal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRatingSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facade test for {@code client.user().ratings()} — lazy paginated streaming
 * (short-page termination, filter-param propagation), rating reads, the answer
 * and removal writes, and the public summary.
 */
@WireMockTest
class UserRatingsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 100;

    private static final String RATINGS_PATH = "/sale/user-ratings";
    private static final String RATING_ID = "5df0a6d1ef437e00255572a1";
    private static final String RATING_BY_ID_PATH = RATINGS_PATH + "/" + RATING_ID;
    private static final String ANSWER_PATH = RATING_BY_ID_PATH + "/answer";
    private static final String REMOVAL_PATH = RATING_BY_ID_PATH + "/removal";
    private static final String USER_ID = "111332841";
    private static final String SUMMARY_PATH = "/users/" + USER_ID + "/ratings-summary";
    private static final String ANSWER_MESSAGE = "Thanks for the feedback";
    private static final String REMOVAL_MESSAGE = "Issue resolved with buyer";
    private static final OffsetDateTime CHANGED_FROM =
            OffsetDateTime.of(2025, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC);

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String RATING_RESPONSE = """
            {"id":"%s","buyer":{"id":"b1","login":"buyer-login"},"recommended":true,
             "comment":"great","createdAt":"2025-01-15T08:36:57.292Z",
             "answer":{"createdAt":"2025-01-16T08:36:57.292Z","message":"you are welcome"},
             "order":{"id":"order-1"}}
            """.formatted(RATING_ID);
    private static final String ANSWER_RESPONSE = """
            {"createdAt":"2025-01-16T08:36:57.292Z","message":"%s"}
            """.formatted(ANSWER_MESSAGE);
    private static final String REMOVAL_RESPONSE = """
            {"possibleTo":"2025-02-01T00:00:00Z",
             "request":{"createdAt":"2025-01-16T08:36:57.292Z","message":"%s","source":"SELLER"}}
            """.formatted(REMOVAL_MESSAGE);
    private static final String SUMMARY_RESPONSE = """
            {"recommended":{"unique":120,"total":130},"notRecommended":{"unique":2,"total":3},
             "recommendedPercentage":"98.5","user":{"createdAt":"2015-06-01"}}
            """;
    // A rating carrying the per-category scores + exclusion reason. `description`
    // holds an out-of-range wire value (9) — an unknown score that must degrade to
    // null, not surface as a bogus number.
    private static final String RATING_FULL_RESPONSE = """
            {"id":"%s","buyer":{"id":"b1","login":"buyer-login"},"recommended":false,
             "createdAt":"2025-01-15T08:36:57.292Z",
             "excludedFromAverageRates":true,"excludedFromAverageRatesReason":"TEST_PURCHASE",
             "rates":{"delivery":5,"deliveryCost":4,"description":9,"service":3},
             "order":{"id":"order-1"}}
            """.formatted(RATING_ID);
    // A removal whose `source` is an unmodelled value — must map to null, never SELLER.
    private static final String REMOVAL_UNKNOWN_SOURCE_RESPONSE = """
            {"possibleTo":"2025-02-01T00:00:00Z",
             "request":{"createdAt":"2025-01-16T08:36:57.292Z","message":"%s","source":"ROBOT"}}
            """.formatted(REMOVAL_MESSAGE);
    private static final String SUMMARY_WITH_STATISTICS_RESPONSE = """
            {"recommended":{"unique":120,"total":130},"notRecommended":{"unique":2,"total":3},
             "recommendedPercentage":"98.5","user":{"createdAt":"2015-06-01"},
             "statistics":{"received":{"total":133},"excluded":{"total":5},
               "removed":{"total":4,"byAdmin":1,"byBuyer":2,"byBuyerDueToCompensation":1}}}
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

    private static String fullPageOfRatings(int count) {
        StringBuilder json = new StringBuilder("{\"ratings\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"r").append(index)
                    .append("\",\"buyer\":{\"id\":\"b\",\"login\":\"l\"},\"recommended\":true,")
                    .append("\"createdAt\":\"2025-01-15T08:36:57.292Z\"}");
        }
        return json.append("]}").toString();
    }

    @Test
    void stream_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page; the second page must not be requested
        stubFor(get(urlPathEqualTo(RATINGS_PATH))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfRatings(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — consume only the first rating
            List<UserRating> firstOnly = allegro.user().ratings().stream(RatingFilter.all())
                    .limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(RATINGS_PATH))
                    .withQueryParam("offset", equalTo("0")));
            verify(0, getRequestedFor(urlPathEqualTo(RATINGS_PATH))
                    .withQueryParam("offset", equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void stream_whenTraversed_terminatesOnShortPageAndReturnsAll(WireMockRuntimeInfo wmInfo) {
        // given — page one full (100), page two short (2) => iteration stops after page two
        stubFor(get(urlPathEqualTo(RATINGS_PATH)).withQueryParam("offset", equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfRatings(PAGE_SIZE))));
        stubFor(get(urlPathEqualTo(RATINGS_PATH))
                .withQueryParam("offset", equalTo(String.valueOf(PAGE_SIZE)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfRatings(2))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.user().ratings().stream(RatingFilter.all()).count();

            // then
            assertEquals(PAGE_SIZE + 2L, total);
            verify(1, getRequestedFor(urlPathEqualTo(RATINGS_PATH))
                    .withQueryParam("offset", equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void stream_whenFilterGiven_sendsRecommendedAndDateQueryParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(RATINGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfRatings(1))));
        RatingFilter filter = RatingFilter.builder()
                .recommended(false)
                .changedFrom(CHANGED_FROM)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.user().ratings().stream(filter).toList();

            // then — filter values propagated as query parameters
            verify(getRequestedFor(urlPathEqualTo(RATINGS_PATH))
                    .withQueryParam("recommended", equalTo("false"))
                    .withQueryParam("lastChangedAt.gte", equalTo(CHANGED_FROM.toString())));
        }
    }

    @Test
    void get_whenRatingExists_mapsBuyerRecommendedAndAnswer(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(RATING_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(RATING_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            UserRating rating = allegro.user().ratings().get(RATING_ID);

            // then
            assertEquals("buyer-login", rating.buyer().login());
            assertTrue(rating.recommended());
            assertEquals("order-1", rating.orderId());
            assertEquals("you are welcome", rating.answer().message());
        }
    }

    @Test
    void answer_whenValid_putsMessageBodyAndMapsResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(ANSWER_PATH))
                .withRequestBody(containing("\"message\":\"" + ANSWER_MESSAGE + "\""))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(ANSWER_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Answer answer = allegro.user().ratings()
                    .answer(RATING_ID, RatingAnswer.builder().message(ANSWER_MESSAGE).build());

            // then
            assertEquals(ANSWER_MESSAGE, answer.message());
            verify(1, putRequestedFor(urlEqualTo(ANSWER_PATH))
                    .withRequestBody(containing("\"message\":\"" + ANSWER_MESSAGE + "\"")));
        }
    }

    @Test
    void requestRemoval_whenValid_putsNestedRequestMessageAndMapsResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(REMOVAL_PATH))
                .withRequestBody(containing("\"request\""))
                .withRequestBody(containing("\"message\":\"" + REMOVAL_MESSAGE + "\""))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(REMOVAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Removal removal = allegro.user().ratings()
                    .requestRemoval(RATING_ID, RatingRemoval.builder().message(REMOVAL_MESSAGE).build());

            // then — nested {request:{message}} sent, response mapped incl. source enum
            assertEquals(REMOVAL_MESSAGE, removal.request().message());
            assertEquals(Removal.Source.SELLER, removal.request().source());
            verify(1, putRequestedFor(urlEqualTo(REMOVAL_PATH)));
        }
    }

    @Test
    void get_whenRatingHasRatesAndExclusion_mapsScoresReasonAndDropsUnknownRate(WireMockRuntimeInfo wmInfo) {
        // given — a rating with per-category scores, an exclusion reason, and one
        // out-of-range (unknown) category score
        stubFor(get(urlEqualTo(RATING_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(RATING_FULL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            UserRating rating = allegro.user().ratings().get(RATING_ID);

            // then — exclusion + reason mapped
            assertTrue(rating.excludedFromAverageRates());
            assertEquals("TEST_PURCHASE", rating.excludedFromAverageRatesReason());
            // and — the valid 1..5 scores map, the unknown one degrades to null
            UserRating.Rates rates = rating.rates();
            assertEquals(5, rates.delivery().intValue());
            assertEquals(4, rates.deliveryCost().intValue());
            assertEquals(3, rates.service().intValue());
            assertNull(rates.description());
        }
    }

    @Test
    void requestRemoval_whenSourceUnknown_mapsToNullWithoutMislabelling(WireMockRuntimeInfo wmInfo) {
        // given — the server reports an unmodelled removal source
        stubFor(put(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(REMOVAL_UNKNOWN_SOURCE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Removal removal = allegro.user().ratings()
                    .requestRemoval(RATING_ID, RatingRemoval.builder().message(REMOVAL_MESSAGE).build());

            // then — an unknown source is null, never silently reported as SELLER
            assertNull(removal.request().source());
        }
    }

    @Test
    void summaryOf_whenStatisticsReturned_mapsReceivedExcludedAndRemovedBreakdown(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SUMMARY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(SUMMARY_WITH_STATISTICS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            UserRatingSummary summary = allegro.user().ratings().summaryOf(USER_ID);

            // then — the full statistics breakdown maps, incl. the removed split
            UserRatingSummary.Statistics stats = summary.statistics();
            assertEquals(133L, stats.receivedTotal());
            assertEquals(5L, stats.excludedTotal());
            assertEquals(4L, stats.removed().total());
            assertEquals(1L, stats.removed().byAdmin());
            assertEquals(2L, stats.removed().byBuyer());
            assertEquals(1L, stats.removed().byBuyerDueToCompensation());
        }
    }

    @Test
    void summaryOf_whenReturned_mapsRecommendedAndNotRecommendedCounts(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SUMMARY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SUMMARY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            UserRatingSummary summary = allegro.user().ratings().summaryOf(USER_ID);

            // then
            assertEquals(120L, summary.recommended().unique());
            assertEquals(130L, summary.recommended().total());
            assertEquals(3L, summary.notRecommended().total());
            assertEquals("98.5", summary.recommendedPercentage());
        }
    }
}
