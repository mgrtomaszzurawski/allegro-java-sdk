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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibilitySuggestionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityInputType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityList;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityListType;
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
 * the {@code suggestionsFor} polymorphic {@code MANUAL}/{@code PRODUCT_BASED}
 * mapping and its unknown-type degrade, the request builder's offer-xor-product
 * guard, the empty-response case, and the mandatory error-path table.
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
    // Wire-verified 2026-07-19 via a client-credentials probe of the sandbox
    // (65 categories: 54 ID + 11 TEXT; every category carried validationRules;
    // maxCharactersPerLine was null for ID inputType and 100 for TEXT). Three
    // categories exercise the whole mapping: an ID category (maxRows set,
    // maxCharactersPerLine null — the live ID shape), a TEXT category (both bounds
    // set), and a synthetic category whose inputType is outside the two modelled
    // values (must degrade to UNKNOWN, not fail the read).
    private static final String SUPPORTED_CATEGORIES = """
            {"supportedCategories":[
              {"categoryId":"620","name":"Czesci samochodowe","itemsType":"CAR",
               "inputType":"ID","validationRules":{"maxRows":2000,"maxCharactersPerLine":null}},
              {"categoryId":"258","name":"Opony i felgi","itemsType":"CAR",
               "inputType":"TEXT","validationRules":{"maxRows":2000,"maxCharactersPerLine":100}},
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

    private static final String CATEGORY_ID_ID_TYPE = "620";
    private static final String CATEGORY_ID_TEXT_TYPE = "258";
    private static final int EXPECTED_MAX_ROWS = 2000;
    private static final int EXPECTED_MAX_CHARS_PER_LINE = 100;

    // ---- suggestionsFor ----

    private static final String SUGGESTIONS_PATH = "/sale/compatibility-list-suggestions";
    private static final String OFFER_ID_PARAM = "offer.id";
    private static final String PRODUCT_ID_PARAM = "product.id";
    private static final String LANGUAGE_PARAM = "language";
    private static final String TEST_OFFER_ID = "12345678";
    private static final String TEST_PRODUCT_ID = "5a1b2c3d-0000-4000-8000-000000000001";
    private static final String TEST_LANGUAGE = "pl-PL";
    private static final String PRODUCT_LIST_ID = "9f8e7d6c";
    private static final String COMPATIBLE_ITEM_ID = "5019";
    private static final String MANUAL_ID_ITEM_TEXT = "BMW X5 (E70) 2007-2013";
    private static final String MANUAL_TEXT_ITEM_TEXT = "Uniwersalny do serii E";
    private static final String ADDITIONAL_INFO_ENGINE = "3.0d";
    private static final String ADDITIONAL_INFO_CODE = "M57";
    private static final String PRODUCT_BASED_ITEM_TEXT = "BMW X6 (E71)";
    private static final int EXPECTED_ITEM_COUNT = 2;

    // spec-derived: not yet wire-verified (buyer/seller-scoped suggestion for a
    // real offer/product; to be confirmed by the bucket's exploration pass). A
    // MANUAL list mixes an ID item (id + label + additionalInfo) and a free-TEXT item.
    private static final String MANUAL_SUGGESTION = """
            {"type":"MANUAL","items":[
              {"type":"ID","id":"%s","text":"%s","additionalInfo":[{"value":"%s"},{"value":"%s"}]},
              {"type":"TEXT","text":"%s"}]}
            """.formatted(COMPATIBLE_ITEM_ID, MANUAL_ID_ITEM_TEXT,
            ADDITIONAL_INFO_ENGINE, ADDITIONAL_INFO_CODE, MANUAL_TEXT_ITEM_TEXT);
    // spec-derived: a PRODUCT_BASED list carries the derived list id and read-only text items.
    private static final String PRODUCT_BASED_SUGGESTION = """
            {"type":"PRODUCT_BASED","id":"%s","items":[{"text":"%s"},{"text":"%s"}]}
            """.formatted(PRODUCT_LIST_ID, MANUAL_ID_ITEM_TEXT, PRODUCT_BASED_ITEM_TEXT);
    // a list type this SDK version does not model — must degrade, not fail the read.
    private static final String UNKNOWN_SUGGESTION = """
            {"type":"FUTURE_KIND","items":[]}
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
            assertEquals("CAR", byId.itemsType());
            assertEquals(CompatibilityInputType.ID, byId.inputType());
            // an ID category caps the list size but has no per-line text bound
            assertEquals(Integer.valueOf(EXPECTED_MAX_ROWS), byId.validationRules().maxRows());
            assertNull(byId.validationRules().maxCharactersPerLine());

            CompatibleCategory byText = categories.get(1);
            assertEquals(CATEGORY_ID_TEXT_TYPE, byText.categoryId());
            assertEquals(CompatibilityInputType.TEXT, byText.inputType());
            assertEquals(Integer.valueOf(EXPECTED_MAX_ROWS), byText.validationRules().maxRows());
            assertEquals(Integer.valueOf(EXPECTED_MAX_CHARS_PER_LINE),
                    byText.validationRules().maxCharactersPerLine());

            // an unmodelled inputType degrades to UNKNOWN rather than failing the read;
            // this category also omits validationRules entirely (absent block -> null)
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

    // ---- suggestionsFor: mapping ----

    @Test
    void suggestionsFor_whenManualList_mapsIdAndTextItemsAndSendsOfferId(WireMockRuntimeInfo wmInfo) {
        // given — a manual suggestion for an offer, localized
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SUGGESTIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(MANUAL_SUGGESTION)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            CompatibilityList list = allegro.catalog().compatibility().suggestionsFor(
                    CompatibilitySuggestionRequest.builder()
                            .offerId(TEST_OFFER_ID).language(TEST_LANGUAGE).build());

            // then — MANUAL, no list id, both item variants mapped
            assertEquals(CompatibilityListType.MANUAL, list.type());
            assertNull(list.id());
            assertEquals(EXPECTED_ITEM_COUNT, list.items().size());

            CompatibilityItem idItem = list.items().get(0);
            assertEquals(CompatibilityInputType.ID, idItem.type());
            assertEquals(COMPATIBLE_ITEM_ID, idItem.id());
            assertEquals(MANUAL_ID_ITEM_TEXT, idItem.text());
            assertEquals(List.of(ADDITIONAL_INFO_ENGINE, ADDITIONAL_INFO_CODE), idItem.additionalInfo());

            CompatibilityItem textItem = list.items().get(1);
            assertEquals(CompatibilityInputType.TEXT, textItem.type());
            assertNull(textItem.id());
            assertEquals(MANUAL_TEXT_ITEM_TEXT, textItem.text());
            assertTrue(textItem.additionalInfo().isEmpty());

            // and the offer id (not product id) and language reached the wire
            verify(1, getRequestedFor(urlPathEqualTo(SUGGESTIONS_PATH))
                    .withQueryParam(OFFER_ID_PARAM, equalTo(TEST_OFFER_ID))
                    .withQueryParam(PRODUCT_ID_PARAM, absent())
                    .withQueryParam(LANGUAGE_PARAM, equalTo(TEST_LANGUAGE)));
        }
    }

    @Test
    void suggestionsFor_whenProductBasedList_mapsListIdAndTextItemsAndSendsProductId(
            WireMockRuntimeInfo wmInfo) {
        // given — a product-based suggestion for a product
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SUGGESTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PRODUCT_BASED_SUGGESTION)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            CompatibilityList list = allegro.catalog().compatibility().suggestionsFor(
                    CompatibilitySuggestionRequest.forProduct(TEST_PRODUCT_ID));

            // then — PRODUCT_BASED, derived list id, text-only items
            assertEquals(CompatibilityListType.PRODUCT_BASED, list.type());
            assertEquals(PRODUCT_LIST_ID, list.id());
            assertEquals(EXPECTED_ITEM_COUNT, list.items().size());
            CompatibilityItem first = list.items().get(0);
            assertEquals(CompatibilityInputType.TEXT, first.type());
            assertNull(first.id());
            assertEquals(MANUAL_ID_ITEM_TEXT, first.text());
            assertEquals(PRODUCT_BASED_ITEM_TEXT, list.items().get(1).text());

            // and the product id (not offer id) reached the wire
            verify(1, getRequestedFor(urlPathEqualTo(SUGGESTIONS_PATH))
                    .withQueryParam(PRODUCT_ID_PARAM, equalTo(TEST_PRODUCT_ID))
                    .withQueryParam(OFFER_ID_PARAM, absent()));
        }
    }

    @Test
    void suggestionsFor_whenUnknownListType_degradesToUnknownWithEmptyItems(WireMockRuntimeInfo wmInfo) {
        // given — a list type Allegro introduced after this SDK version
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SUGGESTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(UNKNOWN_SUGGESTION)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            CompatibilityList list = allegro.catalog().compatibility().suggestionsFor(
                    CompatibilitySuggestionRequest.forOffer(TEST_OFFER_ID));

            // then — degrades rather than failing the whole read
            assertEquals(CompatibilityListType.UNKNOWN, list.type());
            assertNull(list.id());
            assertTrue(list.items().isEmpty());
        }
    }

    @Test
    void suggestionsFor_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SUGGESTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compatibility = allegro.catalog().compatibility();
            CompatibilitySuggestionRequest request = CompatibilitySuggestionRequest.forProduct(TEST_PRODUCT_ID);

            // then — the tree-fetch path surfaces errors like any other GET
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> compatibility.suggestionsFor(request));
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlPathEqualTo(SUGGESTIONS_PATH)));
        }
    }

    // ---- suggestionsFor: request builder offer-xor-product guard ----

    @Test
    void suggestionsFor_whenNeitherOfferNorProduct_throwsIllegalState() {
        // then — a request targeting nothing is rejected fail-fast at build time
        assertThrows(IllegalStateException.class,
                () -> CompatibilitySuggestionRequest.builder().language(TEST_LANGUAGE).build());
    }

    @Test
    void suggestionsFor_whenBothOfferAndProduct_throwsIllegalState() {
        // then — offer and product are mutually exclusive
        assertThrows(IllegalStateException.class,
                () -> CompatibilitySuggestionRequest.builder()
                        .offerId(TEST_OFFER_ID).productId(TEST_PRODUCT_ID).build());
    }
}
