/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the {@code catalog().categories()} facade: vendor
 * headers, {@code parent.id} query wiring, Raw → record mapping, and the
 * mandatory error-path table (400/401/404/429/5xx) against a representative
 * endpoint ({@code get(categoryId)}).
 */
@WireMockTest
class CatalogCategoriesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String TEST_TRACE_ID = "4631702648f0524e";

    private static final String CATEGORIES_PATH = "/sale/categories";
    private static final String CATEGORY_ID = "165929";
    private static final String CHILD_CATEGORY_ID = "709";
    private static final String CATEGORY_BY_ID_PATH = CATEGORIES_PATH + "/" + CATEGORY_ID;
    private static final String PARENT_ID_QUERY = CATEGORIES_PATH + "?parent.id=" + CATEGORY_ID;

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long EXPECTED_RETRY_AFTER = 1L;
    private static final int RETRY_MAX_ATTEMPTS = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // Shape wire-verified on the sandbox 2026-07-17 via the catalog-categories
    // demo (id/name/leaf/parent/options all arrive parseable); the literal
    // values below are illustrative. See KNOWN-SERVER-BEHAVIORS.md.
    private static final String LEAF_CATEGORY = """
            {"id":"%s","name":"Wiertarki","leaf":true,"parent":{"id":"%s"},
             "options":{"advertisement":false,"offersWithProductPublicationEnabled":true,
                        "productCreationEnabled":true,"sellerCanRequirePurchaseComments":false}}
            """.formatted(CATEGORY_ID, CHILD_CATEGORY_ID);
    private static final String ROOT_LIST = """
            {"categories":[
              {"id":"%s","name":"Elektronika","leaf":false},
              {"id":"3","name":"Motoryzacja","leaf":false}]}
            """.formatted(CATEGORY_ID);
    private static final String CHILD_LIST = """
            {"categories":[{"id":"%s","name":"Wiertarki","leaf":true,"parent":{"id":"%s"}}]}
            """.formatted(CATEGORY_ID, CATEGORY_ID);
    private static final String BAD_REQUEST = """
            {"errors":[{"code":"CategoryIdNotValidException",
              "message":"Provided category id is not valid","userMessage":"Nieprawidłowa kategoria",
              "path":"categoryId"}]}
            """;
    private static final String NOT_FOUND = """
            {"errors":[{"code":"CategoryNotFoundException","message":"Category not found",
              "userMessage":"Nie znaleziono kategorii","path":null}]}
            """;
    private static final String BUSY = "{\"errors\":[]}";

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

    /** Retry with the server's Retry-After floor capped to 0 so tests don't sleep whole seconds. */
    private static RetryPolicy fastRetry() {
        return RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).maxRetryAfterSeconds(0).build();
    }

    private static void stubToken(String accessToken) {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    // ---- happy paths ----

    @Test
    void get_whenCategoryExists_sendsVendorHeadersAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(LEAF_CATEGORY)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Category category = allegro.catalog().categories().get(CATEGORY_ID);

            // then — Raw mapped to the immutable domain record, nested options included
            assertEquals(CATEGORY_ID, category.id());
            assertEquals("Wiertarki", category.name());
            assertTrue(category.leaf());
            assertEquals(CHILD_CATEGORY_ID, category.parentId());
            assertNotNull(category.options());
            assertTrue(category.options().productCreationEnabled());
            assertFalse(category.options().advertisement());
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
        }
    }

    @Test
    void roots_whenCalled_requestsCategoriesWithoutParentFilterAndMapsList(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(ROOT_LIST)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<Category> roots = allegro.catalog().categories().roots();

            // then — a root carries no parent, no options block
            assertEquals(2, roots.size());
            assertEquals("Elektronika", roots.get(0).name());
            assertFalse(roots.get(0).leaf());
            assertNull(roots.get(0).parentId());
            assertNull(roots.get(0).options());
            // no parent.id parameter on the wire
            verify(1, getRequestedFor(urlPathEqualTo(CATEGORIES_PATH))
                    .withQueryParam("parent.id", absent()));
        }
    }

    @Test
    void childrenOf_whenCalled_sendsParentIdQueryAndMapsChildren(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PARENT_ID_QUERY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CHILD_LIST)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<Category> children = allegro.catalog().categories().childrenOf(CATEGORY_ID);

            // then — the parent.id filter reached the wire, the child mapped back
            assertEquals(1, children.size());
            assertEquals(CATEGORY_ID, children.get(0).parentId());
            assertTrue(children.get(0).leaf());
            verify(1, getRequestedFor(urlPathEqualTo(CATEGORIES_PATH))
                    .withQueryParam("parent.id", equalTo(CATEGORY_ID)));
        }
    }

    @Test
    void roots_whenResponseHasNoCategories_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — a categories envelope with the array omitted
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("{}")));

        try (AllegroClient allegro = client(wmInfo)) {
            // then — never null
            assertTrue(allegro.catalog().categories().roots().isEmpty());
        }
    }

    // ---- mandatory error-path table (against get(categoryId)) ----

    @Test
    void get_when400WithErrors_throwsBadRequestWithParsedFieldError(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST)));

        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();

            // then — typed field errors survive; 400 is not retried
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> categories.get(CATEGORY_ID));
            assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, failure.statusCode());
            assertFalse(failure.errors().isEmpty());
            assertEquals("CategoryIdNotValidException", failure.errors().get(0).code());
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(LEAF_CATEGORY)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Category category = allegro.catalog().categories().get(CATEGORY_ID);

            // then — replayed exactly once, the replay carried the fresh token
            assertEquals(CATEGORY_ID, category.id());
            verify(2, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, () -> categories.get(CATEGORY_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
        }
    }

    @Test
    void get_when429Persists_retriesThenThrowsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — a GET is retried; every attempt is throttled with Retry-After: 1
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                String.valueOf(EXPECTED_RETRY_AFTER))
                        .withBody(BUSY)));

        try (AllegroClient allegro = client(wmInfo, fastRetry())) {
            var categories = allegro.catalog().categories();

            // then — retried to exhaustion, then the typed rate-limit with the server's hint
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, () -> categories.get(CATEGORY_ID));
            assertEquals(EXPECTED_RETRY_AFTER, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
        }
    }

    @Test
    void get_when500Persists_retriesThenThrowsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo, fastRetry())) {
            var categories = allegro.catalog().categories();

            // then — a GET IS retried on 5xx (unlike POST), to exhaustion
            AllegroServerException failure =
                    assertThrows(AllegroServerException.class, () -> categories.get(CATEGORY_ID));
            assertEquals(TestHttpConstants.HTTP_SERVER_ERROR, failure.statusCode());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
        }
    }
}
