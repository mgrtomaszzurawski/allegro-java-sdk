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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the {@code catalog().products()} facade: vendor headers,
 * search query wiring, Raw → {@link ProductSummary} mapping, cursor-pagination
 * laziness ({@code page.id}), and the mandatory error-path table.
 */
@WireMockTest
class CatalogProductsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_REAUTH = "token-two";
    private static final String TEST_TRACE_ID = "4631702648f0524e";

    private static final String PRODUCTS_PATH = "/sale/products";
    private static final String PHRASE = "iphone";
    private static final String CATEGORY_ID = "257";
    private static final String CURSOR_2 = "CURSOR2";
    private static final String PHRASE_PARAM = "phrase";
    private static final String CATEGORY_PARAM = "category.id";
    private static final String PAGE_ID_PARAM = "page.id";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long EXPECTED_RETRY_AFTER = 1L;
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final int PAGE_ONE_SIZE = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // Shapes spec-derived; wire-verified via the catalog-products demo (see
    // KNOWN-SERVER-BEHAVIORS.md). Page 1 carries a nextPage cursor; page 2 does not.
    private static final String PAGE_ONE = """
            {"products":[
              {"id":"P-1","name":"iPhone 15","category":{"id":"%s"},
               "images":[{"url":"https://img.allegro/1.jpg"},{"url":"https://img.allegro/2.jpg"}]},
              {"id":"P-2","name":"iPhone 15 Pro","category":{"id":"%s"},"images":[]}],
             "nextPage":{"id":"%s"}}
            """.formatted(CATEGORY_ID, CATEGORY_ID, CURSOR_2);
    private static final String PAGE_TWO = """
            {"products":[
              {"id":"P-3","name":"iPhone 15 Plus","category":{"id":"%s"},
               "images":[{"url":"https://img.allegro/3.jpg"}]}]}
            """.formatted(CATEGORY_ID);
    private static final String BAD_REQUEST = """
            {"errors":[{"code":"SearchQueryNotValidException",
              "message":"Provided phrase is too short","userMessage":"Zbyt krótka fraza",
              "path":"phrase"}]}
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

    private static RetryPolicy fastRetry() {
        return RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).maxRetryAfterSeconds(0).build();
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    // ---- search: mapping + pagination ----

    @Test
    void search_whenFirstPageReturned_mapsSummariesWithImagesAndCategory(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withQueryParam(PHRASE_PARAM, equalTo(PHRASE))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PAGE_ONE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — take only the first page's worth
            List<ProductSummary> summaries = allegro.catalog().products()
                    .search(ProductSearchRequest.byPhrase(PHRASE))
                    .limit(PAGE_ONE_SIZE)
                    .toList();

            // then
            assertEquals(PAGE_ONE_SIZE, summaries.size());
            ProductSummary first = summaries.get(0);
            assertEquals("P-1", first.id());
            assertEquals("iPhone 15", first.name());
            assertEquals(CATEGORY_ID, first.categoryId());
            assertEquals(2, first.imageUrls().size());
            assertEquals("https://img.allegro/1.jpg", first.imageUrls().get(0));
            // an empty images array maps to an empty list, never null
            assertTrue(summaries.get(1).imageUrls().isEmpty());
        }
    }

    @Test
    void search_whenConsumerStopsAfterFirstPage_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — page 1 advertises a next cursor; page 2 is stubbed but must not be hit
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withQueryParam(PAGE_ID_PARAM, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PAGE_ONE)));
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(CURSOR_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PAGE_TWO)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — limit exactly to page one
            long count = allegro.catalog().products()
                    .search(ProductSearchRequest.byPhrase(PHRASE))
                    .limit(PAGE_ONE_SIZE)
                    .count();

            // then — laziness: the second page was never requested
            assertEquals(PAGE_ONE_SIZE, count);
            verify(1, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH))
                    .withQueryParam(PAGE_ID_PARAM, absent()));
            verify(0, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH))
                    .withQueryParam(PAGE_ID_PARAM, equalTo(CURSOR_2)));
        }
    }

    @Test
    void search_whenFullyConsumed_followsCursorAndKeepsFiltersAcrossPages(WireMockRuntimeInfo wmInfo) {
        // given — two pages; both requests must carry the phrase + category filters
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withQueryParam(PAGE_ID_PARAM, absent())
                .withQueryParam(PHRASE_PARAM, equalTo(PHRASE))
                .withQueryParam(CATEGORY_PARAM, equalTo(CATEGORY_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PAGE_ONE)));
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(CURSOR_2))
                .withQueryParam(PHRASE_PARAM, equalTo(PHRASE))
                .withQueryParam(CATEGORY_PARAM, equalTo(CATEGORY_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PAGE_TWO)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — drain the whole stream
            List<ProductSummary> all = allegro.catalog().products()
                    .search(ProductSearchRequest.builder().phrase(PHRASE).categoryId(CATEGORY_ID).build())
                    .toList();

            // then — both pages, in order, then the stream ended (page 2 had no next cursor)
            assertEquals(3, all.size());
            assertEquals("P-3", all.get(2).id());
            verify(1, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH))
                    .withQueryParam(PAGE_ID_PARAM, equalTo(CURSOR_2))
                    .withQueryParam(CATEGORY_PARAM, equalTo(CATEGORY_ID)));
        }
    }

    // ---- mandatory error-path table (against search) ----

    @Test
    void search_when400WithErrors_throwsBadRequestWithParsedFieldError(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST)));

        try (AllegroClient allegro = client(wmInfo)) {
            var products = allegro.catalog().products();
            ProductSearchRequest request = ProductSearchRequest.byPhrase(PHRASE);

            // then — the terminal op triggers the fetch, which raises the typed error
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> products.search(request).toList());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals("SearchQueryNotValidException", failure.errors().get(0).code());
        }
    }

    @Test
    void search_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
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
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH)).withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_REAUTH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PAGE_TWO)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<ProductSummary> summaries = allegro.catalog().products()
                    .search(ProductSearchRequest.byPhrase(PHRASE)).toList();

            // then — replayed once with the fresh token
            assertEquals(1, summaries.size());
            verify(2, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH)));
            verify(1, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_REAUTH)));
        }
    }

    @Test
    void search_when429Persists_retriesThenThrowsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                String.valueOf(EXPECTED_RETRY_AFTER))
                        .withBody(BUSY)));

        try (AllegroClient allegro = client(wmInfo, fastRetry())) {
            var products = allegro.catalog().products();
            ProductSearchRequest request = ProductSearchRequest.byPhrase(PHRASE);

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> products.search(request).toList());
            assertEquals(EXPECTED_RETRY_AFTER, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH)));
        }
    }

    @Test
    void search_when500Persists_retriesThenThrowsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PRODUCTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo, fastRetry())) {
            var products = allegro.catalog().products();
            ProductSearchRequest request = ProductSearchRequest.byPhrase(PHRASE);

            // then — a GET is retried on 5xx, to exhaustion
            assertThrows(AllegroServerException.class, () -> products.search(request).toList());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(PRODUCTS_PATH)));
        }
    }

    // ---- request builder ----

    @Test
    void builder_byPhrase_setsOnlyPhrase() {
        // when
        ProductSearchRequest request = ProductSearchRequest.byPhrase(PHRASE);

        // then
        assertEquals(PHRASE, request.phrase());
        assertNull(request.categoryId());
        assertNull(request.language());
    }

    @Test
    void builder_allCoreFieldsSet_areReadableAndSurviveToBuilder() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .phrase(PHRASE).categoryId(CATEGORY_ID).language("en-US").build();

        // when
        ProductSearchRequest copy = request.toBuilder().build();

        // then
        assertEquals(PHRASE, copy.phrase());
        assertEquals(CATEGORY_ID, copy.categoryId());
        assertEquals("en-US", copy.language());
    }

    @Test
    void builder_inCategory_setsOnlyCategory() {
        // when
        ProductSearchRequest request = ProductSearchRequest.inCategory(CATEGORY_ID);

        // then
        assertEquals(CATEGORY_ID, request.categoryId());
        assertNull(request.phrase());
    }

    @Test
    void builder_whenNoPhraseNorCategory_throwsIllegalState() {
        // then — a criterion-less search is rejected before any call
        assertThrows(IllegalStateException.class, () -> ProductSearchRequest.builder().build());
        assertThrows(IllegalStateException.class,
                () -> ProductSearchRequest.builder().phrase("  ").build());
    }
}
