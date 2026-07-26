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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParameterRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CategoryEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEventType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameterType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategorySuggestion;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    private static final String TEST_TOKEN_REAUTH = "token-two";
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
    private static final String MINIMAL_CATEGORY = """
            {"id":"%s","name":"Elektronika"}
            """.formatted(CATEGORY_ID);
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

    private static final String CATEGORY_PARAMETERS_PATH = CATEGORY_BY_ID_PATH + "/parameters";
    private static final String MATCHING_PATH = "/sale/matching-categories";
    private static final String NAME_PARAM = "name";
    private static final String SUGGEST_NAME = "iphone";
    private static final String NO_MATCHES = "{\"matchingCategories\":[]}";
    private static final String NULL_MATCHES = "{\"matchingCategories\":null}";
    private static final String UNKNOWN_TYPE_PARAM =
            "{\"parameters\":[{\"id\":\"1\",\"name\":\"Geo\",\"type\":\"geo\"}]}";
    // Shapes spec-derived; wire-verified on the sandbox via the catalog-categories
    // demo (see KNOWN-SERVER-BEHAVIORS.md). One parameter of each of the four types.
    private static final String CATEGORY_PARAMETERS = """
            {"parameters":[
              {"id":"11323","name":"Marka","type":"dictionary","required":true,"requiredForProduct":true,
               "options":{"describesProduct":true,"customValuesEnabled":false,
                          "ambiguousValueId":"11323_0","dependsOnParameterId":null},
               "restrictions":{"multipleChoices":true},
               "dictionary":[{"id":"11323_1","value":"Bosch","dependsOnValueIds":[]},
                             {"id":"11323_2","value":"Makita","dependsOnValueIds":["11323_1"]}]},
              {"id":"medium","name":"Moc","type":"float","required":false,"requiredForProduct":false,
               "unit":"W","restrictions":{"min":0.0,"max":2000.5,"range":false,"precision":2}},
              {"id":"count","name":"Liczba sztuk","type":"integer","required":false,
               "requiredForProduct":false,"restrictions":{"min":1,"max":100,"range":true}},
              {"id":"note","name":"Opis","type":"string","required":false,"requiredForProduct":false,
               "restrictions":{"minLength":0,"maxLength":40,"allowedNumberOfValues":1}}]}
            """;
    private static final String MATCHING_CATEGORIES = """
            {"matchingCategories":[
              {"id":"257","name":"Smartfony i telefony komórkowe",
               "parent":{"id":"48978","name":"Telefony i Akcesoria",
                         "parent":{"id":"165","name":"Elektronika"}}},
              {"id":"321","name":"Akcesoria GSM"}]}
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

    /** Retry with the server's Retry-After floor capped to 0 so tests don't sleep whole seconds. */
    private static RetryPolicy fastRetry() {
        return RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).maxRetryAfterSeconds(0).build();
    }

    private static void stubToken(String accessToken) {
        stubFor(post(
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
            // when
            List<Category> roots = allegro.catalog().categories().roots();

            // then — never null, and the call still hit the wire
            assertTrue(roots.isEmpty());
            verify(1, getRequestedFor(urlEqualTo(CATEGORIES_PATH)));
        }
    }

    @Test
    void get_whenOptionalFieldsOmitted_defaultsLeafFalseAndNullsParentAndOptions(
            WireMockRuntimeInfo wmInfo) {
        // given — a minimal category: no leaf, no parent, no options (spec marks none required)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(MINIMAL_CATEGORY)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Category category = allegro.catalog().categories().get(CATEGORY_ID);

            // then — an absent boolean maps to false (not an unboxing NPE), refs stay null
            assertFalse(category.leaf());
            assertNull(category.parentId());
            assertNull(category.options());
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
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertFalse(failure.errors().isEmpty());
            assertEquals("CategoryIdNotValidException", failure.errors().get(0).code());
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_REAUTH, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(CATEGORY_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_REAUTH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(LEAF_CATEGORY)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Category category = allegro.catalog().categories().get(CATEGORY_ID);

            // then — replayed exactly once, the replay carried the fresh token
            assertEquals(CATEGORY_ID, category.id());
            verify(2, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH)));
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_BY_ID_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_REAUTH)));
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

    // ---- fail-fast input validation ----

    @Test
    void childrenOf_whenIdNull_throwsNullPointerExceptionInsteadOfDegradingToRoots(
            WireMockRuntimeInfo wmInfo) {
        // then — a null id must fail loudly, never silently query all roots
        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();
            assertThrows(NullPointerException.class, () -> categories.childrenOf(null));
        }
    }

    @Test
    void get_whenIdNull_throwsNullPointerException(WireMockRuntimeInfo wmInfo) {
        // then
        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();
            assertThrows(NullPointerException.class, () -> categories.get(null));
        }
    }

    // ---- category parameters ----

    @Test
    void parameters_whenCategoryHasParameters_mapsEveryTypeAndItsRestrictions(
            WireMockRuntimeInfo wmInfo) {
        // given — one parameter of each type: dictionary, float, integer, string
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_PARAMETERS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CATEGORY_PARAMETERS)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CategoryParameter> parameters =
                    allegro.catalog().categories().parameters(CATEGORY_ID);

            // then — the polymorphic Raw hierarchy flattened onto the typed record
            assertEquals(4, parameters.size());

            CategoryParameter dictionary = parameters.get(0);
            assertEquals(CategoryParameterType.DICTIONARY, dictionary.type());
            assertTrue(dictionary.required());
            assertTrue(dictionary.requiredForProduct());
            assertNotNull(dictionary.options());
            assertTrue(dictionary.options().describesProduct());
            assertEquals("11323_0", dictionary.options().ambiguousValueId());
            assertNull(dictionary.options().dependsOnParameterId());
            assertNotNull(dictionary.restrictions());
            assertTrue(dictionary.restrictions().multipleChoices());
            assertEquals(2, dictionary.dictionary().size());
            assertEquals("11323_1", dictionary.dictionary().get(0).id());
            assertEquals("Bosch", dictionary.dictionary().get(0).value());
            assertEquals(List.of("11323_1"), dictionary.dictionary().get(1).dependsOnValueIds());

            CategoryParameter floatParam = parameters.get(1);
            assertEquals(CategoryParameterType.FLOAT, floatParam.type());
            assertEquals("W", floatParam.unit());
            assertNotNull(floatParam.restrictions());
            BigDecimal floatMin = floatParam.restrictions().minValue();
            assertNotNull(floatMin);
            assertEquals(0, floatMin.compareTo(new BigDecimal("0.0")));
            BigDecimal floatMax = floatParam.restrictions().maxValue();
            assertNotNull(floatMax);
            assertEquals(0, floatMax.compareTo(new BigDecimal("2000.5")));
            assertEquals(Integer.valueOf(2), floatParam.restrictions().precision());
            assertTrue(floatParam.dictionary().isEmpty());

            CategoryParameter integerParam = parameters.get(2);
            assertEquals(CategoryParameterType.INTEGER, integerParam.type());
            assertNotNull(integerParam.restrictions());
            BigDecimal integerMin = integerParam.restrictions().minValue();
            assertNotNull(integerMin);
            assertEquals(0, integerMin.compareTo(BigDecimal.ONE));
            BigDecimal integerMax = integerParam.restrictions().maxValue();
            assertNotNull(integerMax);
            assertEquals(0, integerMax.compareTo(new BigDecimal("100")));
            assertTrue(integerParam.restrictions().range());
            assertNull(integerParam.restrictions().precision());

            CategoryParameter stringParam = parameters.get(3);
            assertEquals(CategoryParameterType.STRING, stringParam.type());
            assertNotNull(stringParam.restrictions());
            assertEquals(Integer.valueOf(40), stringParam.restrictions().maxLength());

            verify(1, getRequestedFor(urlEqualTo(CATEGORY_PARAMETERS_PATH)));
        }
    }

    @Test
    void parameters_whenResponseHasNoParameters_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — a parameter envelope with the array omitted
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_PARAMETERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("{}")));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CategoryParameter> parameters =
                    allegro.catalog().categories().parameters(CATEGORY_ID);

            // then — never null, and the call still hit the wire
            assertTrue(parameters.isEmpty());
            verify(1, getRequestedFor(urlEqualTo(CATEGORY_PARAMETERS_PATH)));
        }
    }

    @Test
    void parameters_whenIdNull_throwsNullPointerException(WireMockRuntimeInfo wmInfo) {
        // then
        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();
            assertThrows(NullPointerException.class, () -> categories.parameters(null));
        }
    }

    @Test
    void from_whenParameterTypeIsUnmodeled_mapsToOtherWithoutRestrictionsOrDictionary() {
        // given — a base parameter DTO whose concrete type this SDK version does not
        // model. This exercises the mapper's OTHER default DIRECTLY; note the live wire
        // cannot yet produce it (Jackson rejects an unknown discriminator before the
        // mapper runs — see parameters_whenTypeUnknown_... and CategoryParameterType.OTHER).
        CategoryParameterRaw raw = new CategoryParameterRaw();
        raw.setId("999");
        raw.setName("Nowość");
        raw.setType("geo");
        raw.setRequired(true);

        // when
        CategoryParameter parameter = CategoryParameter.from(raw);

        // then
        assertEquals(CategoryParameterType.OTHER, parameter.type());
        assertTrue(parameter.required());
        assertNull(parameter.restrictions());
        assertTrue(parameter.dictionary().isEmpty());
    }

    @Test
    void parameters_whenTypeUnknown_degradesToOtherRatherThanFailing(
            WireMockRuntimeInfo wmInfo) {
        // given — a parameter whose discriminator is outside the four modelled types.
        // The generated Raw base has no @JsonSubTypes defaultImpl, so the core
        // UnknownSubtypeToBaseHandler deserializes it as the polymorphic base and the
        // domain mapper lands it on CategoryParameterType.OTHER instead of failing the
        // whole response (BACKLOG C4 — core forward-compat for discriminated subtypes).
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CATEGORY_PARAMETERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_TYPE_PARAM)));

        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();
            // when
            List<CategoryParameter> parameters = categories.parameters(CATEGORY_ID);
            // then — the unmodelled parameter survives, mapped to OTHER, not thrown
            assertEquals(1, parameters.size());
            assertEquals(CategoryParameterType.OTHER, parameters.get(0).type());
        }
    }

    // ---- category suggestions (matching-categories) ----

    @Test
    void suggest_whenNameMatches_sendsNameQueryAndMapsParentChain(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(MATCHING_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(MATCHING_CATEGORIES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CategorySuggestion> suggestions =
                    allegro.catalog().categories().suggest(SUGGEST_NAME);

            // then — best match first, with its breadcrumb reachable via parent()
            assertEquals(2, suggestions.size());
            CategorySuggestion best = suggestions.get(0);
            assertEquals("257", best.id());
            assertEquals("Smartfony i telefony komórkowe", best.name());
            CategorySuggestion parent = best.parent();
            assertNotNull(parent);
            assertEquals("48978", parent.id());
            CategorySuggestion grandParent = parent.parent();
            assertNotNull(grandParent);
            assertEquals("Elektronika", grandParent.name());
            assertNull(grandParent.parent());
            // a root match carries no parent
            assertNull(suggestions.get(1).parent());
            // the required name filter reached the wire
            verify(1, getRequestedFor(urlPathEqualTo(MATCHING_PATH))
                    .withQueryParam(NAME_PARAM, equalTo(SUGGEST_NAME)));
        }
    }

    @Test
    void suggest_whenNoMatches_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(MATCHING_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(NO_MATCHES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // then
            assertTrue(allegro.catalog().categories().suggest(SUGGEST_NAME).isEmpty());
            verify(1, getRequestedFor(urlPathEqualTo(MATCHING_PATH)));
        }
    }

    @Test
    void suggest_whenMatchingCategoriesNull_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — an explicit null array (the impl's null-guard branch, distinct from [])
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(MATCHING_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(NULL_MATCHES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // then — never null
            assertTrue(allegro.catalog().categories().suggest(SUGGEST_NAME).isEmpty());
        }
    }

    @Test
    void suggest_whenNameNull_throwsNullPointerException(WireMockRuntimeInfo wmInfo) {
        // then — a null name must fail loudly, never match every category
        try (AllegroClient allegro = client(wmInfo)) {
            var categories = allegro.catalog().categories();
            assertThrows(NullPointerException.class, () -> categories.suggest(null));
        }
    }

    // ---- streamChanges (category-events feed) ----

    private static final String CATEGORY_EVENTS_PATH = "/sale/category-events";
    private static final String FROM_QUERY = "from";
    private static final String LIMIT_QUERY = "limit";
    private static final String TYPE_QUERY = "type";
    private static final int EVENTS_PAGE_SIZE = 100;
    private static final String EVENT_ID_CREATED = "e1";
    private static final String EVENT_ID_DELETED = "e2";
    private static final String CREATED_CATEGORY_ID = "C1";
    private static final String DELETED_REDIRECT_ID = "C9";
    private static final String RESUME_CURSOR = "evt-42";
    private static final int EXPECTED_EVENT_COUNT = 5;
    private static final int EXPECTED_SECOND_PAGE = 2;

    // A small page (fewer than the page size) ends the stream. Covers all four
    // modelled event types plus one type this SDK version does not model. The common
    // event shape (id/occurredAt/type/category{id,name,leaf,parent}) is wire-verified
    // 2026-07-19 (sandbox, client-credentials; the live sample was all CATEGORY_CREATED);
    // the DELETED redirectCategory and MOVED parent are spec-derived (not in that sample).
    private static final String EVENTS_PAGE = """
            {"events":[
              {"type":"CATEGORY_CREATED","id":"e1","occurredAt":"2026-07-01T10:00:00Z",
               "category":{"id":"C1","name":"Nowa","leaf":true,"parent":{"id":"P1"}}},
              {"type":"CATEGORY_DELETED","id":"e2","occurredAt":"2026-07-01T10:01:00Z",
               "category":{"id":"C2","name":"Stara"},"redirectCategory":{"id":"C9"}},
              {"type":"CATEGORY_MOVED","id":"e3","occurredAt":"2026-07-01T10:02:00Z",
               "category":{"id":"C3","name":"Przeniesiona","parent":{"id":"P2"}}},
              {"type":"CATEGORY_RENAMED","id":"e4","occurredAt":"2026-07-01T10:03:00Z",
               "category":{"id":"C4","name":"Nowa nazwa"}},
              {"type":"CATEGORY_SPLIT","id":"e5","occurredAt":"2026-07-01T10:04:00Z"}]}
            """;

    @Test
    void streamChanges_whenPageHasAllTypesPlusUnmodelled_mapsEachAndDegradesUnknown(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(EVENTS_PAGE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<CategoryEvent> events = allegro.catalog().categories()
                    .streamChanges(CategoryEventFilter.all()).toList();

            // then — every event maps; the affected category and redirect land per type
            assertEquals(EXPECTED_EVENT_COUNT, events.size());

            CategoryEvent created = events.get(0);
            assertEquals(EVENT_ID_CREATED, created.id());
            assertEquals(CategoryEventType.CATEGORY_CREATED, created.type());
            assertNotNull(created.occurredAt());
            assertEquals(CREATED_CATEGORY_ID, created.category().id());
            assertEquals("Nowa", created.category().name());
            assertTrue(created.category().leaf());
            assertEquals("P1", created.category().parentId());
            assertNull(created.redirectCategoryId());

            CategoryEvent deleted = events.get(1);
            assertEquals(EVENT_ID_DELETED, deleted.id());
            assertEquals(CategoryEventType.CATEGORY_DELETED, deleted.type());
            assertEquals(DELETED_REDIRECT_ID, deleted.redirectCategoryId());
            assertNull(deleted.category().parentId());

            assertEquals(CategoryEventType.CATEGORY_MOVED, events.get(2).type());
            assertEquals(CategoryEventType.CATEGORY_RENAMED, events.get(3).type());

            // an unmodelled event type degrades rather than failing the stream
            CategoryEvent unknown = events.get(4);
            assertEquals(CategoryEventType.UNKNOWN, unknown.type());
            assertNull(unknown.category());
            assertNull(unknown.redirectCategoryId());

            // the first fetch sends no `from` cursor and the page-size limit
            verify(1, getRequestedFor(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                    .withQueryParam(FROM_QUERY, absent())
                    .withQueryParam(LIMIT_QUERY, equalTo(String.valueOf(EVENTS_PAGE_SIZE))));
        }
    }

    @Test
    void streamChanges_isLazy_doesNotFetchPageTwoUntilConsumed(WireMockRuntimeInfo wmInfo) {
        // given — a full page (so the cursor advances to the last event id) then an empty page
        stubToken(TEST_TOKEN);
        String lastId = "e" + (EVENTS_PAGE_SIZE - 1);
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .withQueryParam(FROM_QUERY, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(fullEventsPage())));
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .withQueryParam(FROM_QUERY, equalTo(lastId))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("{\"events\":[]}")));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — a bounded consumer takes only the first page's worth
            List<CategoryEvent> firstPage = allegro.catalog().categories()
                    .streamChanges(CategoryEventFilter.all())
                    .limit(EVENTS_PAGE_SIZE).toList();

            // then — page two (from the last event id) is never fetched
            assertEquals(EVENTS_PAGE_SIZE, firstPage.size());
            verify(0, getRequestedFor(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                    .withQueryParam(FROM_QUERY, equalTo(lastId)));
        }
    }

    @Test
    void streamChanges_whenConsumedPastFirstPage_advancesFromCursorToLastEventId(
            WireMockRuntimeInfo wmInfo) {
        // given — a full first page (cursor advances to e99) then a short second page
        stubToken(TEST_TOKEN);
        String lastId = "e" + (EVENTS_PAGE_SIZE - 1);
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .withQueryParam(FROM_QUERY, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(fullEventsPage())));
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .withQueryParam(FROM_QUERY, equalTo(lastId))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("""
                        {"events":[
                          {"type":"CATEGORY_CREATED","id":"e100","occurredAt":"2026-07-01T11:00:00Z",
                           "category":{"id":"C100","name":"c"}},
                          {"type":"CATEGORY_CREATED","id":"e101","occurredAt":"2026-07-01T11:00:01Z",
                           "category":{"id":"C101","name":"c"}}]}
                        """)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — a full consumer drains both pages
            long total = allegro.catalog().categories()
                    .streamChanges(CategoryEventFilter.all()).count();

            // then — the second page was fetched with the last event id as the `from` cursor
            assertEquals(EVENTS_PAGE_SIZE + EXPECTED_SECOND_PAGE, total);
            verify(1, getRequestedFor(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                    .withQueryParam(FROM_QUERY, equalTo(lastId)));
        }
    }

    private static String fullEventsPage() {
        return "{\"events\":[" + IntStream.range(0, EVENTS_PAGE_SIZE)
                .mapToObj(index -> "{\"type\":\"CATEGORY_CREATED\",\"id\":\"e" + index
                        + "\",\"occurredAt\":\"2026-07-01T10:00:00Z\","
                        + "\"category\":{\"id\":\"C" + index + "\",\"name\":\"c\"}}")
                .collect(Collectors.joining(",")) + "]}";
    }

    @Test
    void streamChanges_whenTypesFilter_sendsRepeatedTypeQuery(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("{\"events\":[]}")));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.catalog().categories().streamChanges(
                    CategoryEventFilter.ofTypes(
                            CategoryEventType.CATEGORY_MOVED, CategoryEventType.CATEGORY_RENAMED))
                    .toList();

            // then — both wire type values are sent as repeated query params
            verify(1, getRequestedFor(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                    .withQueryParam(TYPE_QUERY, equalTo("CATEGORY_MOVED"))
                    .withQueryParam(TYPE_QUERY, equalTo("CATEGORY_RENAMED")));
        }
    }

    @Test
    void streamChanges_whenSince_sendsFromCursorOnFirstPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("{\"events\":[]}")));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — resume after a known event id
            allegro.catalog().categories()
                    .streamChanges(CategoryEventFilter.since(RESUME_CURSOR)).toList();

            // then — the first page carries that id as the `from` cursor
            verify(1, getRequestedFor(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                    .withQueryParam(FROM_QUERY, equalTo(RESUME_CURSOR)));
        }
    }

    @Test
    void streamChanges_when400_throwsBadRequestOnConsumption(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CATEGORY_EVENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST)));

        try (AllegroClient allegro = client(wmInfo)) {
            var stream = allegro.catalog().categories().streamChanges(CategoryEventFilter.all());

            // then — the lazy stream surfaces the error when first consumed
            assertThrows(AllegroBadRequestException.class, stream::toList);
        }
    }
}
