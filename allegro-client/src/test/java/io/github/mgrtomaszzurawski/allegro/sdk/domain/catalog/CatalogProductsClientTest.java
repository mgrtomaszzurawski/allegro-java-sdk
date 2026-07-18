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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameterType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Product;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the {@code catalog().products()} facade: vendor headers,
 * search query wiring, Raw → {@link ProductSummary} mapping, cursor-pagination
 * laziness ({@code page.id}), the polymorphic {@link ProductParameter} mapping
 * behind {@code parametersIn} (including the C4 unknown-type degrade), and the
 * mandatory error-path table.
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
    private static final String NO_PHRASE_HINT = "requires a phrase";

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
               "publication":{"status":"LISTED"},
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

    private static final String PRODUCT_ID = "5272069b-0759-4283-8ba7-7f0512345678";
    private static final String PRODUCT_BY_ID_PATH = PRODUCTS_PATH + "/" + PRODUCT_ID;
    private static final String PRODUCT = """
            {"id":"%s","name":"iPhone 15 128GB","category":{"id":"%s"},
             "publication":{"status":"LISTED"},"hasProtectedBrand":true,
             "images":[{"url":"https://img.allegro/p.jpg"}],
             "parameters":[
               {"id":"11323","name":"Marka","values":["Apple"],"valuesIds":["11323_1"]},
               {"id":"pojemnosc","name":"Pojemność","values":["128 GB"],"unit":"GB"}]}
            """.formatted(PRODUCT_ID, CATEGORY_ID);
    // A bare product: category present but with no id, and every optional block omitted.
    private static final String MINIMAL_PRODUCT = """
            {"id":"%s","name":"Bare product","category":{}}
            """.formatted(PRODUCT_ID);
    private static final String NOT_FOUND = """
            {"errors":[{"code":"ProductNotFoundException","message":"Product not found",
              "userMessage":"Nie znaleziono produktu","path":null}]}
            """;

    private static final String PRODUCT_PARAMETERS_PATH =
            "/sale/categories/" + CATEGORY_ID + "/product-parameters";
    // One parameter of each modelled wire type, so the polymorphic mapping and the
    // by-type restriction/dictionary flattening are all exercised. The STRING/DICTIONARY
    // shapes were wire-verified 2026-07-18 via the catalog-products demo (parametersIn(353)
    // returned 18 real product parameters mapping cleanly); the FLOAT/INTEGER restriction
    // shapes remain spec-derived (category 353 carried no numeric params live) but are
    // structurally identical to the already-shipped category-parameter variants.
    private static final String PRODUCT_PARAMETERS = """
            {"parameters":[
              {"id":"1","name":"Marka","type":"dictionary","required":true,
               "restrictions":{"multipleChoices":true},
               "dictionary":[{"id":"1_1","value":"Apple"},{"id":"1_2","value":"Samsung"}]},
              {"id":"2","name":"Przekatna ekranu","type":"float","required":false,"unit":"cal",
               "restrictions":{"min":1.0,"max":100.0,"range":false,"precision":2}},
              {"id":"3","name":"Liczba rdzeni","type":"integer","required":false,
               "restrictions":{"min":1,"max":64,"range":false}},
              {"id":"4","name":"Model","type":"string","required":true,
               "restrictions":{"minLength":1,"maxLength":50,"allowedNumberOfValues":1}}]}
            """;
    // A product parameter whose discriminator is outside the four modelled types.
    private static final String PRODUCT_PARAMETERS_UNKNOWN_TYPE = """
            {"parameters":[{"id":"9","name":"New kind","type":"quantum","required":false}]}
            """;
    private static final String CATEGORY_NOT_FOUND = """
            {"errors":[{"code":"NotFound","message":"Category not found",
              "userMessage":"Nie znaleziono kategorii","path":null}]}
            """;
    private static final String EMPTY_JSON_OBJECT = "{}";

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
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
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
            assertEquals("LISTED", first.publicationStatus());
            assertEquals(2, first.imageUrls().size());
            assertEquals("https://img.allegro/1.jpg", first.imageUrls().get(0));
            // an empty images array maps to an empty list, never null; absent publication → null
            assertTrue(summaries.get(1).imageUrls().isEmpty());
            assertNull(summaries.get(1).publicationStatus());
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
            List<ProductSummary> allSummaries = allegro.catalog().products()
                    .search(ProductSearchRequest.builder().phrase(PHRASE).categoryId(CATEGORY_ID).build())
                    .toList();

            // then — both pages, in order, then the stream ended (page 2 had no next cursor)
            assertEquals(3, allSummaries.size());
            assertEquals("P-3", allSummaries.get(2).id());
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
    void builder_whenNoPhrase_throwsIllegalStateWithMessage() {
        // then — a phrase is required, so an empty request is rejected with a helpful message
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ProductSearchRequest.builder().build());
        assertTrue(failure.getMessage().contains(NO_PHRASE_HINT));
        // a blank phrase counts as absent
        assertThrows(IllegalStateException.class,
                () -> ProductSearchRequest.builder().phrase("  ").build());
    }

    @Test
    void builder_whenCategoryButNoPhrase_throwsIllegalState() {
        // then — the spec forbids category-only search (category filters a phrase search)
        assertThrows(IllegalStateException.class,
                () -> ProductSearchRequest.builder().categoryId(CATEGORY_ID).build());
    }

    // ---- get(productId) ----

    @Test
    void get_whenProductExists_mapsRecordAndParameterValues(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PRODUCT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Product product = allegro.catalog().products().get(PRODUCT_ID);

            // then — identity, classification, flags, images and parameter values map
            assertEquals(PRODUCT_ID, product.id());
            assertEquals("iPhone 15 128GB", product.name());
            assertEquals(CATEGORY_ID, product.categoryId());
            assertEquals("LISTED", product.publicationStatus());
            assertTrue(product.hasProtectedBrand());
            assertEquals(1, product.imageUrls().size());
            assertEquals(2, product.parameters().size());
            assertEquals("11323", product.parameters().get(0).id());
            assertEquals(List.of("Apple"), product.parameters().get(0).values());
            assertEquals(List.of("11323_1"), product.parameters().get(0).valuesIds());
            assertNull(product.parameters().get(0).unit());
            assertEquals("GB", product.parameters().get(1).unit());
            assertEquals(List.of("128 GB"), product.parameters().get(1).values());
            assertTrue(product.parameters().get(1).valuesIds().isEmpty());
            verify(1, getRequestedFor(urlEqualTo(PRODUCT_BY_ID_PATH)));
        }
    }

    @Test
    void get_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given — completes the facade's error-path table (search cannot 404; get can)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            var products = allegro.catalog().products();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, () -> products.get(PRODUCT_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlEqualTo(PRODUCT_BY_ID_PATH)));
        }
    }

    @Test
    void get_whenIdNull_throwsNullPointerExceptionFromTheGuard(WireMockRuntimeInfo wmInfo) {
        // then — the fail-fast guard (not an incidental deref) rejects a null id
        try (AllegroClient allegro = client(wmInfo)) {
            var products = allegro.catalog().products();
            NullPointerException failure =
                    assertThrows(NullPointerException.class, () -> products.get(null));
            assertTrue(failure.getMessage().contains("productId"));
        }
    }

    @Test
    void get_whenOptionalFieldsOmitted_defaultsFalseAndNulls(WireMockRuntimeInfo wmInfo) {
        // given — a bare product: category with no id, and no publication/brand/images/parameters
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(MINIMAL_PRODUCT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Product product = allegro.catalog().products().get(PRODUCT_ID);

            // then — absent boolean → false (not an unboxing NPE), absent refs → null, lists empty
            assertNull(product.categoryId());
            assertNull(product.publicationStatus());
            assertFalse(product.hasProtectedBrand());
            assertTrue(product.imageUrls().isEmpty());
            assertTrue(product.parameters().isEmpty());
        }
    }

    // ---- parametersIn(categoryId) ----

    @Test
    void parametersIn_whenCategoryHasParameters_mapsEachPolymorphicType(WireMockRuntimeInfo wmInfo) {
        // given — one parameter of each modelled wire type
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_PARAMETERS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PRODUCT_PARAMETERS)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<ProductParameter> parameters =
                    allegro.catalog().products().parametersIn(CATEGORY_ID);

            // then — one record per wire type, restrictions/dictionary flattened by type
            assertEquals(4, parameters.size());

            ProductParameter dictionary = parameters.get(0);
            assertEquals(CategoryParameterType.DICTIONARY, dictionary.type());
            assertTrue(dictionary.required());
            assertNull(dictionary.unit());
            // a non-default restriction value proves it is read, not defaulted
            assertTrue(dictionary.restrictions().multipleChoices());
            assertEquals(2, dictionary.dictionary().size());
            assertEquals("Apple", dictionary.dictionary().get(0).value());
            // product-side dictionary values carry no combination dependencies
            assertTrue(dictionary.dictionary().get(0).dependsOnValueIds().isEmpty());

            ProductParameter floatParam = parameters.get(1);
            assertEquals(CategoryParameterType.FLOAT, floatParam.type());
            assertFalse(floatParam.required());
            assertEquals("cal", floatParam.unit());
            assertEquals(0, new BigDecimal("1.0").compareTo(floatParam.restrictions().minValue()));
            assertEquals(0, new BigDecimal("100.0").compareTo(floatParam.restrictions().maxValue()));
            assertEquals(Integer.valueOf(2), floatParam.restrictions().precision());
            assertTrue(floatParam.dictionary().isEmpty());

            ProductParameter integerParam = parameters.get(2);
            assertEquals(CategoryParameterType.INTEGER, integerParam.type());
            assertEquals(0, new BigDecimal("64").compareTo(integerParam.restrictions().maxValue()));
            // integers carry no decimal precision
            assertNull(integerParam.restrictions().precision());

            ProductParameter stringParam = parameters.get(3);
            assertEquals(CategoryParameterType.STRING, stringParam.type());
            assertEquals(Integer.valueOf(50), stringParam.restrictions().maxLength());
            assertEquals(Integer.valueOf(1), stringParam.restrictions().allowedNumberOfValues());
            verify(1, getRequestedFor(urlEqualTo(PRODUCT_PARAMETERS_PATH)));
        }
    }

    @Test
    void parametersIn_whenTypeUnknown_degradesToOtherRatherThanFailing(WireMockRuntimeInfo wmInfo) {
        // given — a discriminator outside the four modelled types. The core
        // UnknownSubtypeToBaseHandler resolves it to the polymorphic base (the Raw
        // declares no defaultImpl), so the mapper lands it on OTHER instead of failing
        // the whole response (BACKLOG C4 — forward-compat for discriminated subtypes).
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_PARAMETERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PRODUCT_PARAMETERS_UNKNOWN_TYPE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<ProductParameter> parameters =
                    allegro.catalog().products().parametersIn(CATEGORY_ID);

            // then — the unmodelled parameter survives, mapped to OTHER, not thrown
            assertEquals(1, parameters.size());
            assertEquals(CategoryParameterType.OTHER, parameters.get(0).type());
            assertNull(parameters.get(0).restrictions());
            assertTrue(parameters.get(0).dictionary().isEmpty());
            verify(1, getRequestedFor(urlEqualTo(PRODUCT_PARAMETERS_PATH)));
        }
    }

    @Test
    void parametersIn_whenNoParameters_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — a category that defines no product parameters (absent array)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_PARAMETERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(EMPTY_JSON_OBJECT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // then — an absent array maps to an empty list, never null
            assertTrue(allegro.catalog().products().parametersIn(CATEGORY_ID).isEmpty());
            verify(1, getRequestedFor(urlEqualTo(PRODUCT_PARAMETERS_PATH)));
        }
    }

    @Test
    void parametersIn_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given — this method addresses a distinct path (a category), so it has its own 404
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PRODUCT_PARAMETERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(CATEGORY_NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            var products = allegro.catalog().products();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> products.parametersIn(CATEGORY_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlEqualTo(PRODUCT_PARAMETERS_PATH)));
        }
    }

    @Test
    void parametersIn_whenCategoryIdNull_throwsNullPointerExceptionFromTheGuard(
            WireMockRuntimeInfo wmInfo) {
        // then — the fail-fast guard (not an incidental deref) rejects a null id
        try (AllegroClient allegro = client(wmInfo)) {
            var products = allegro.catalog().products();
            NullPointerException failure =
                    assertThrows(NullPointerException.class, () -> products.parametersIn(null));
            assertTrue(failure.getMessage().contains("categoryId"));
        }
    }
}
