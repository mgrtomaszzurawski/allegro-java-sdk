/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityInputType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the {@code catalog().compatibility()} facade: vendor
 * headers, Raw → {@link CompatibleCategory} mapping (per-{@link
 * CompatibilityInputType} rules, the enum degrade for an unmodelled input type),
 * the empty-response case, and the mandatory error-path table.
 */
@WireMockTest
class CompatibilityClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_REAUTH = "token-two";
    private static final String TEST_TRACE_ID = "4631702648f0524e";

    private static final String SUPPORTED_CATEGORIES_PATH =
            "/sale/compatibility-list/supported-categories";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long EXPECTED_RETRY_AFTER = 1L;
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final int EXPECTED_CATEGORY_COUNT = 3;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // Spec-derived; the shape is wire-verifiable through the catalog-compatibility demo.
    // Three categories exercise the whole mapping: an ID category (no rules), a TEXT
    // category with validation rules, and a category whose inputType is outside the two
    // modelled values (must degrade to UNKNOWN, not fail the read).
    private static final String SUPPORTED_CATEGORIES = """
            {"supportedCategories":[
              {"categoryId":"257","name":"Czesci samochodowe","itemsType":"VEHICLE",
               "inputType":"ID"},
              {"categoryId":"258","name":"Opony i felgi","itemsType":"VEHICLE",
               "inputType":"TEXT","validationRules":{"maxRows":10,"maxCharactersPerLine":80}},
              {"categoryId":"259","name":"Nowy rodzaj","itemsType":"OTHER",
               "inputType":"HOLOGRAM"}]}
            """;
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final String BAD_REQUEST = """
            {"errors":[{"code":"ValidationException","message":"Invalid Accept-Language",
              "userMessage":"Niepoprawny naglowek","path":null}]}
            """;
    private static final String NOT_FOUND = """
            {"errors":[{"code":"NotFound","message":"Resource not found",
              "userMessage":"Nie znaleziono","path":null}]}
            """;
    private static final String BUSY = "{\"errors\":[]}";

    private static final String CATEGORY_ID_ID_TYPE = "257";
    private static final String CATEGORY_ID_TEXT_TYPE = "258";
    private static final int EXPECTED_MAX_ROWS = 10;
    private static final int EXPECTED_MAX_CHARS_PER_LINE = 80;

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

    private static RetryPolicy fastRetry() {
        return RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).maxRetryAfterSeconds(0).build();
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    // ---- supportedCategories: mapping ----

    @Test
    void supportedCategories_whenCategoriesReturned_mapsInputTypeAndRulesAndDegradesUnknown(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(SUPPORTED_CATEGORIES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CompatibleCategory> categories = allegro.catalog().compatibility()
                    .supportedCategories();

            // then — all three map; input type and validation rules land per category
            assertEquals(EXPECTED_CATEGORY_COUNT, categories.size());

            CompatibleCategory byId = categories.get(0);
            assertEquals(CATEGORY_ID_ID_TYPE, byId.categoryId());
            assertEquals("Czesci samochodowe", byId.name());
            assertEquals("VEHICLE", byId.itemsType());
            assertEquals(CompatibilityInputType.ID, byId.inputType());
            // an ID category carries no free-text bounds
            assertNull(byId.validationRules());

            CompatibleCategory byText = categories.get(1);
            assertEquals(CATEGORY_ID_TEXT_TYPE, byText.categoryId());
            assertEquals(CompatibilityInputType.TEXT, byText.inputType());
            assertEquals(Integer.valueOf(EXPECTED_MAX_ROWS), byText.validationRules().maxRows());
            assertEquals(Integer.valueOf(EXPECTED_MAX_CHARS_PER_LINE),
                    byText.validationRules().maxCharactersPerLine());

            // an unmodelled inputType degrades to UNKNOWN rather than failing the read
            CompatibleCategory unknown = categories.get(2);
            assertEquals(CompatibilityInputType.UNKNOWN, unknown.inputType());
            assertNull(unknown.validationRules());
            verify(1, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH)));
        }
    }

    @Test
    void supportedCategories_whenNoCategories_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — the array is absent entirely
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(EMPTY_JSON_OBJECT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CompatibleCategory> categories = allegro.catalog().compatibility()
                    .supportedCategories();

            // then — never null, an empty list
            assertTrue(categories.isEmpty());
            verify(1, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH)));
        }
    }

    // ---- mandatory error-path table ----

    @Test
    void supportedCategories_when400WithErrors_throwsBadRequestWithParsedFieldError(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compatibility = allegro.catalog().compatibility();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    compatibility::supportedCategories);
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals("ValidationException", failure.errors().get(0).code());
        }
    }

    @Test
    void supportedCategories_when401Once_reauthenticatesAndReplaysWithFreshToken(
            WireMockRuntimeInfo wmInfo) {
        // given — token-one then token-two on re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_REAUTH, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_REAUTH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(EMPTY_JSON_OBJECT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CompatibleCategory> categories = allegro.catalog().compatibility()
                    .supportedCategories();

            // then — replayed once with the fresh token
            assertTrue(categories.isEmpty());
            verify(2, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH)));
            verify(1, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_REAUTH)));
        }
    }

    @Test
    void supportedCategories_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compatibility = allegro.catalog().compatibility();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    compatibility::supportedCategories);
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH)));
        }
    }

    @Test
    void supportedCategories_when429Persists_retriesThenThrowsRateLimitWithRetryAfter(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                String.valueOf(EXPECTED_RETRY_AFTER))
                        .withBody(BUSY)));

        try (AllegroClient allegro = client(wmInfo, fastRetry())) {
            var compatibility = allegro.catalog().compatibility();

            // then — a GET is retried on 429, honouring Retry-After, to exhaustion
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    compatibility::supportedCategories);
            assertEquals(EXPECTED_RETRY_AFTER, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH)));
        }
    }

    @Test
    void supportedCategories_when500Persists_retriesThenThrowsServerException(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(SUPPORTED_CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo, fastRetry())) {
            var compatibility = allegro.catalog().compatibility();

            // then — a GET is retried on 5xx, to exhaustion
            assertThrows(AllegroServerException.class, compatibility::supportedCategories);
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(SUPPORTED_CATEGORIES_PATH)));
        }
    }
}
