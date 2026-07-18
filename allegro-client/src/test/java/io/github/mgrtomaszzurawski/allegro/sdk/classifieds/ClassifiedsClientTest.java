/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.classifieds;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder.ClassifiedStatsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedEventType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifiedStats;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifieds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.SellerClassifiedStats;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the classifieds facade. Pins the {@code category.id}
 * query, the single-package and offer-assignment reads, the {@code PUT} that
 * assigns packages (request body verified), the vendor headers, the Raw → record
 * mapping, and the mandatory error-path table (400 typed field errors, 401
 * replay, 404, 429 with Retry-After, 5xx retry) exercised on
 * {@code availablePackages} as the facade's representative endpoint.
 */
@WireMockTest
class ClassifiedsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String PACKAGES_PATH = "/sale/classifieds-packages";
    private static final String CATEGORY_ID_PARAM = "category.id";
    private static final String TEST_CATEGORY_ID = "3928";
    private static final String TEST_PACKAGE_ID = "6174be19-56f9-484b-b72c-43b0b00785e8";
    private static final String TEST_EXTRA_PACKAGE_ID = "3b2f0c11-0000-4a5e-a55c-bcb8e7d53cbb";
    private static final String TEST_OFFER_ID = "8235476198";
    private static final String PACKAGE_PATH = PACKAGES_PATH + "/" + TEST_PACKAGE_ID;
    private static final String OFFER_PACKAGES_PATH = "/sale/offer-classifieds-packages/" + TEST_OFFER_ID;
    private static final String OFFER_STATS_PATH = "/sale/classified-offers-stats";
    private static final String SELLER_STATS_PATH = "/sale/classified-seller-stats";
    private static final String OFFER_ID_PARAM = "offer.id";
    private static final String DATE_GTE_PARAM = "date.gte";
    private static final String DATE_LTE_PARAM = "date.lte";
    private static final String TEST_OFFER_ID_2 = "8235476199";
    private static final OffsetDateTime TEST_FROM = OffsetDateTime.parse("2026-07-01T10:15:30Z");
    private static final OffsetDateTime TEST_TO = OffsetDateTime.parse("2026-07-08T10:15:30Z");
    private static final int SHOWED_PHONE_TOTAL = 5;
    private static final int ASKED_QUESTION_DAY = 2;
    private static final int FAVOURITES_TOTAL = 9;
    private static final String TEST_STAT_DAY = "2026-07-01";
    private static final int OVER_MAX_OFFER_IDS = 51;
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_5XX_RECOVERY = "5xx-recovery";
    private static final String STATE_RECOVERED = "recovered";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long RETRY_AFTER_SECONDS = 1L;
    private static final int FAST_MAX_ATTEMPTS = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified. Two packages exercise both enum
    // values, present/empty extensions and promotions, and present/null
    // publication. The classifieds demo pass replaces this with a sandbox
    // capture (TESTING.md §1 fixture provenance).
    private static final String PACKAGES_RESPONSE = """
            {"packages":[
              {"id":"6174be19-56f9-484b-b72c-43b0b00785e8","name":"Power","type":"BASE",
               "extensions":[{"name":"autocentrumExport","description":"Autocentrum.pl"}],
               "promotions":[{"name":"emphasized","duration":"PT240H"}],
               "publication":{"duration":"PT720H"}},
              {"id":"3b2f0c11-0000-4a5e-a55c-bcb8e7d53cbb","name":"Extra","type":"EXTRA",
               "extensions":[],"promotions":[],"publication":null}
            ]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape).
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ConstraintViolationException",
              "message":"category.id is required","userMessage":"Wymagane","path":"category.id",
              "details":null}]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape).
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFoundException","message":"Category not found",
              "userMessage":"Nie znaleziono","path":null}]}
            """;
    // spec-derived: a malformed duration must surface as a typed SDK exception.
    private static final String MALFORMED_DURATION_RESPONSE = """
            {"packages":[
              {"id":"bad","name":"Bad","type":"BASE","extensions":[],
               "promotions":[{"name":"emphasized","duration":"not-a-duration"}],
               "publication":null}
            ]}
            """;
    // spec-derived: not yet wire-verified. A single ClassifiedPackageConfig, the
    // shape returned by GET /sale/classifieds-packages/{packageId}.
    private static final String SINGLE_PACKAGE_RESPONSE = """
            {"id":"%s","name":"Power","type":"BASE",
             "extensions":[{"name":"autocentrumExport","description":"Autocentrum.pl"}],
             "promotions":[{"name":"emphasized","duration":"PT240H"}],
             "publication":{"duration":"PT720H"}}
            """.formatted(TEST_PACKAGE_ID);
    // spec-derived: not yet wire-verified. A ClassifiedResponse — one base
    // package plus one extra carrying the republish flag.
    private static final String OFFER_PACKAGES_RESPONSE = """
            {"basePackage":{"id":"%s"},
             "extraPackages":[{"id":"%s","republish":true}]}
            """.formatted(TEST_PACKAGE_ID, TEST_EXTRA_PACKAGE_ID);
    // The exact body assignPackages must PUT for a base + one republishing extra.
    private static final String ASSIGN_REQUEST_BODY = """
            {"basePackage":{"id":"%s"},
             "extraPackages":[{"id":"%s","republish":true}]}
            """.formatted(TEST_PACKAGE_ID, TEST_EXTRA_PACKAGE_ID);
    // spec-derived: not yet wire-verified. OfferStatsResponseDto — one requested
    // offer with a per-event total and a single day's breakdown.
    private static final String OFFER_STATS_RESPONSE = """
            {"offerStats":[
              {"offer":{"id":"%s"},
               "eventStatsTotal":[{"eventType":"SHOWED_PHONE_NUMBER","count":%d}],
               "eventsPerDay":[{"date":"%s",
                 "eventStats":[{"eventType":"ASKED_QUESTION","count":%d}]}]}
            ]}
            """.formatted(TEST_OFFER_ID, SHOWED_PHONE_TOTAL, TEST_STAT_DAY, ASKED_QUESTION_DAY);
    // spec-derived: not yet wire-verified. SellerOfferStatsResponseDto — bare
    // totals + per-day breakdown, no offer wrapper.
    private static final String SELLER_STATS_RESPONSE = """
            {"eventStatsTotal":[{"eventType":"ADDED_TO_FAVOURITES","count":%d}],
             "eventsPerDay":[{"date":"%s",
               "eventStats":[{"eventType":"SHOWED_PHONE_NUMBER","count":%d}]}]}
            """.formatted(FAVOURITES_TOTAL, TEST_STAT_DAY, SHOWED_PHONE_TOTAL);
    // spec-derived: a malformed daily date must surface as a typed SDK exception.
    private static final String MALFORMED_DATE_STATS_RESPONSE = """
            {"eventStatsTotal":[],
             "eventsPerDay":[{"date":"not-a-date","eventStats":[]}]}
            """;

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

    private static RetryPolicy oneRetry() {
        return RetryPolicy.builder().maxAttempts(FAST_MAX_ATTEMPTS).build();
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    @Test
    void availablePackages_whenCategoryGiven_sendsCategoryQueryAndMapsPackages(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .withQueryParam(CATEGORY_ID_PARAM, equalTo(TEST_CATEGORY_ID))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PACKAGES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<ClassifiedPackage> packages = allegro.classifieds().availablePackages(TEST_CATEGORY_ID);

            // then — both packages mapped, enums/durations/nullability resolved
            assertEquals(2, packages.size());
            ClassifiedPackage base = packages.get(0);
            assertEquals("6174be19-56f9-484b-b72c-43b0b00785e8", base.id());
            assertEquals("Power", base.name());
            assertEquals(ClassifiedPackageType.BASE, base.type());
            assertEquals("autocentrumExport", base.extensions().get(0).name());
            assertEquals(Duration.ofHours(240), base.promotions().get(0).duration());
            assertEquals(Duration.ofHours(720), base.publication().duration());
            ClassifiedPackage extra = packages.get(1);
            assertEquals(ClassifiedPackageType.EXTRA, extra.type());
            assertTrue(extra.extensions().isEmpty());
            assertTrue(extra.promotions().isEmpty());
            assertNull(extra.publication());
            verify(1, getRequestedFor(urlPathEqualTo(PACKAGES_PATH))
                    .withQueryParam(CATEGORY_ID_PARAM, equalTo(TEST_CATEGORY_ID)));
        }
    }

    @Test
    void availablePackages_whenCategoryIdNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — fail-fast on the required input, no token or resource call
            assertThrows(NullPointerException.class, () -> classifieds.availablePackages(null));
            verify(0, getRequestedFor(urlPathEqualTo(PACKAGES_PATH)));
        }
    }

    @Test
    void availablePackages_when400WithErrors_throwsBadRequestWithParsedFieldErrors(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — the typed field errors survive from the errors[] payload
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> classifieds.availablePackages(TEST_CATEGORY_ID));
            assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, failure.statusCode());
            List<AllegroFieldError> errors = failure.errors();
            assertEquals(1, errors.size());
            assertEquals("ConstraintViolationException", errors.get(0).code());
            assertEquals(CATEGORY_ID_PARAM, errors.get(0).path());
        }
    }

    @Test
    void availablePackages_when401Once_reauthenticatesAndReplaysWithFreshToken(
            WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PACKAGES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<ClassifiedPackage> packages = allegro.classifieds().availablePackages(TEST_CATEGORY_ID);

            // then — replayed once, the replay carried the fresh token
            assertEquals(2, packages.size());
            verify(2, getRequestedFor(urlPathEqualTo(PACKAGES_PATH)));
            verify(1, getRequestedFor(urlPathEqualTo(PACKAGES_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void availablePackages_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> classifieds.availablePackages(TEST_CATEGORY_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void availablePackages_when429WithRetryAfter_retriesThenThrowsRateLimit(
            WireMockRuntimeInfo wmInfo) {
        // given — always 429; one retry (maxAttempts=2) then the typed failure
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                String.valueOf(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {
            var classifieds = allegro.classifieds();

            // then — retry happened (2 attempts), Retry-After surfaced
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> classifieds.availablePackages(TEST_CATEGORY_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(PACKAGES_PATH)));
        }
    }

    @Test
    void availablePackages_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first 500, retry returns 200
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PACKAGES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {

            // when
            List<ClassifiedPackage> packages = allegro.classifieds().availablePackages(TEST_CATEGORY_ID);

            // then — the retry recovered the call
            assertEquals(2, packages.size());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(PACKAGES_PATH)));
        }
    }

    @Test
    void availablePackages_whenDurationMalformed_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given — a package whose promotion carries an unparseable duration
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(MALFORMED_DURATION_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — a raw DateTimeParseException never escapes the SDK surface
            assertThrows(AllegroServerException.class,
                    () -> classifieds.availablePackages(TEST_CATEGORY_ID));
        }
    }

    @Test
    void getPackage_whenPackageIdGiven_readsFromPathAndMapsPackage(WireMockRuntimeInfo wmInfo) {
        // given — the package id is a path segment, not a query parameter
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PACKAGE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(SINGLE_PACKAGE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            ClassifiedPackage packageConfig = allegro.classifieds().getPackage(TEST_PACKAGE_ID);

            // then — the single package is mapped from the path resource
            assertEquals(TEST_PACKAGE_ID, packageConfig.id());
            assertEquals("Power", packageConfig.name());
            assertEquals(ClassifiedPackageType.BASE, packageConfig.type());
            assertEquals(Duration.ofHours(720), packageConfig.publication().duration());
            verify(1, getRequestedFor(urlPathEqualTo(PACKAGE_PATH)));
        }
    }

    @Test
    void getPackage_whenPackageIdNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — fail-fast on the required input
            assertThrows(NullPointerException.class, () -> classifieds.getPackage(null));
            verify(0, getRequestedFor(urlPathEqualTo(PACKAGE_PATH)));
        }
    }

    @Test
    void packagesOfOffer_whenOfferIdGiven_mapsBaseAndExtraPackages(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_PACKAGES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(OFFER_PACKAGES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferClassifieds assigned = allegro.classifieds().packagesOfOffer(TEST_OFFER_ID);

            // then — base id and the extra (with its republish flag) both survive
            assertEquals(TEST_PACKAGE_ID, assigned.basePackageId());
            assertEquals(1, assigned.extraPackages().size());
            assertEquals(TEST_EXTRA_PACKAGE_ID, assigned.extraPackages().get(0).id());
            assertEquals(Boolean.TRUE, assigned.extraPackages().get(0).republish());
            verify(1, getRequestedFor(urlPathEqualTo(OFFER_PACKAGES_PATH)));
        }
    }

    @Test
    void packagesOfOffer_whenOfferIdNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then
            assertThrows(NullPointerException.class, () -> classifieds.packagesOfOffer(null));
            verify(0, getRequestedFor(urlPathEqualTo(OFFER_PACKAGES_PATH)));
        }
    }

    @Test
    void assignPackages_whenAssignmentGiven_putsBaseAndExtraBody(WireMockRuntimeInfo wmInfo) {
        // given — the PUT returns no content
        stubToken(TEST_TOKEN);
        stubFor(put(urlPathEqualTo(OFFER_PACKAGES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(equalToJson(ASSIGN_REQUEST_BODY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        ClassifiedAssignment assignment = ClassifiedAssignment.builder()
                .basePackage(TEST_PACKAGE_ID)
                .addExtraPackage(TEST_EXTRA_PACKAGE_ID, true)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.classifieds().assignPackages(TEST_OFFER_ID, assignment);

            // then — the write went out exactly once with the assignment body
            verify(1, putRequestedFor(urlPathEqualTo(OFFER_PACKAGES_PATH))
                    .withRequestBody(equalToJson(ASSIGN_REQUEST_BODY)));
        }
    }

    @Test
    void assignPackages_whenOfferIdNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        ClassifiedAssignment assignment = ClassifiedAssignment.builder()
                .basePackage(TEST_PACKAGE_ID).build();

        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — the offer id is required, fail-fast before the wire
            assertThrows(NullPointerException.class,
                    () -> classifieds.assignPackages(null, assignment));
            verify(0, putRequestedFor(urlPathEqualTo(OFFER_PACKAGES_PATH)));
        }
    }

    @Test
    void assignPackages_whenAssignmentNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — the assignment is required, fail-fast before the wire
            assertThrows(NullPointerException.class,
                    () -> classifieds.assignPackages(TEST_OFFER_ID, null));
            verify(0, putRequestedFor(urlPathEqualTo(OFFER_PACKAGES_PATH)));
        }
    }

    @Test
    void offerStats_whenOffersAndRangeGiven_sendsIdArrayAndRangeAndMaps(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_STATS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(OFFER_STATS_RESPONSE)));
        ClassifiedStatsFilter filter = ClassifiedStatsFilter.builder()
                .eventsFrom(TEST_FROM).eventsTo(TEST_TO).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when — two offer ids requested as a repeated query parameter
            List<OfferClassifiedStats> stats = allegro.classifieds()
                    .offerStats(List.of(TEST_OFFER_ID, TEST_OFFER_ID_2), filter);

            // then — the per-offer totals and per-day breakdown are mapped
            assertEquals(1, stats.size());
            OfferClassifiedStats offerStats = stats.get(0);
            assertEquals(TEST_OFFER_ID, offerStats.offerId());
            assertEquals(ClassifiedEventType.SHOWED_PHONE_NUMBER, offerStats.totals().get(0).eventType());
            assertEquals(SHOWED_PHONE_TOTAL, offerStats.totals().get(0).count());
            assertEquals(LocalDate.parse(TEST_STAT_DAY), offerStats.perDay().get(0).date());
            assertEquals(ASKED_QUESTION_DAY, offerStats.perDay().get(0).events().get(0).count());
            // both ids and both date bounds went out on the wire
            verify(1, getRequestedFor(urlPathEqualTo(OFFER_STATS_PATH))
                    .withQueryParam(OFFER_ID_PARAM, havingExactly(TEST_OFFER_ID, TEST_OFFER_ID_2))
                    .withQueryParam(DATE_GTE_PARAM, equalTo(TEST_FROM.toString()))
                    .withQueryParam(DATE_LTE_PARAM, equalTo(TEST_TO.toString())));
        }
    }

    @Test
    void offerStats_whenOfferIdsNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then
            assertThrows(NullPointerException.class,
                    () -> classifieds.offerStats(null, ClassifiedStatsFilter.all()));
            verify(0, getRequestedFor(urlPathEqualTo(OFFER_STATS_PATH)));
        }
    }

    @Test
    void offerStats_whenOfferIdsEmpty_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — offer.id is required and must carry at least one id
            assertThrows(IllegalArgumentException.class,
                    () -> classifieds.offerStats(List.of(), ClassifiedStatsFilter.all()));
            verify(0, getRequestedFor(urlPathEqualTo(OFFER_STATS_PATH)));
        }
    }

    @Test
    void offerStats_whenOfferIdsExceedMax_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        List<String> tooMany = Collections.nCopies(OVER_MAX_OFFER_IDS, TEST_OFFER_ID);

        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — the server caps offer.id at 50, so the SDK rejects 51 fail-fast
            assertThrows(IllegalArgumentException.class,
                    () -> classifieds.offerStats(tooMany, ClassifiedStatsFilter.all()));
            verify(0, getRequestedFor(urlPathEqualTo(OFFER_STATS_PATH)));
        }
    }

    @Test
    void sellerStats_whenRangeGiven_sendsRangeAndMaps(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SELLER_STATS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(SELLER_STATS_RESPONSE)));
        ClassifiedStatsFilter filter = ClassifiedStatsFilter.builder()
                .eventsFrom(TEST_FROM).eventsTo(TEST_TO).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SellerClassifiedStats stats = allegro.classifieds().sellerStats(filter);

            // then — totals + per-day are mapped, and the date range went out
            assertEquals(ClassifiedEventType.ADDED_TO_FAVOURITES, stats.totals().get(0).eventType());
            assertEquals(FAVOURITES_TOTAL, stats.totals().get(0).count());
            assertEquals(SHOWED_PHONE_TOTAL, stats.perDay().get(0).events().get(0).count());
            verify(1, getRequestedFor(urlPathEqualTo(SELLER_STATS_PATH))
                    .withQueryParam(DATE_GTE_PARAM, equalTo(TEST_FROM.toString()))
                    .withQueryParam(DATE_LTE_PARAM, equalTo(TEST_TO.toString())));
        }
    }

    @Test
    void sellerStats_whenFilterNull_throwsBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then
            assertThrows(NullPointerException.class, () -> classifieds.sellerStats(null));
            verify(0, getRequestedFor(urlPathEqualTo(SELLER_STATS_PATH)));
        }
    }

    @Test
    void sellerStats_whenDailyDateMalformed_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given — a daily bucket carrying an unparseable date
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SELLER_STATS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(MALFORMED_DATE_STATS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var classifieds = allegro.classifieds();

            // then — a raw DateTimeParseException never escapes the SDK surface
            assertThrows(AllegroServerException.class,
                    () -> classifieds.sellerStats(ClassifiedStatsFilter.all()));
        }
    }
}
