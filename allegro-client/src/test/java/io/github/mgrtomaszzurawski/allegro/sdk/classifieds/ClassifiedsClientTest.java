/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.classifieds;

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
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the classifieds facade — the starter slice of bucket F.
 * Pins the {@code category.id} query, the vendor headers, the Raw → record
 * mapping, and the mandatory error-path table (400 typed field errors,
 * 401 replay, 404, 429 with Retry-After, 5xx retry).
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
}
