/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Marketplace;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facade test for {@code client.marketplaces()} — starter slice of bucket D.
 * Proves the accessor wiring, the vendor Accept header, the Raw→record mapping
 * (including omitted optional groups), and the mandatory error table
 * (TESTING.md §1) against {@code GET /marketplaces}.
 */
@WireMockTest
class MarketplacesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String MARKETPLACES_PATH = "/marketplaces";
    // Response shape wire-verified against the sandbox 2026-07-17 (6 marketplaces;
    // allegro-pl base PLN, 25 shipping countries) — trimmed to two entries here.
    private static final String MARKETPLACES_FIXTURE = "account/marketplaces.json";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String SCENARIO_RECOVER = "recover-5xx";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String STATE_RECOVERED = "recovered";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final String RETRY_AFTER_SHORT_VALUE = "1";
    private static final long RETRY_AFTER_SHORT_SECONDS = 1L;

    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String MARKETPLACE_MINIMAL = "allegro-sk";
    private static final String CURRENCY_PLN = "PLN";
    private static final String CURRENCY_EUR = "EUR";
    private static final String CURRENCY_CZK = "CZK";
    private static final String LANGUAGE_PL = "pl-PL";
    private static final String LANGUAGE_EN = "en-US";
    private static final String LANGUAGE_UK = "uk-UA";
    private static final String SHIPPING_COUNTRY_PL = "PL";
    private static final String SHIPPING_COUNTRY_CZ = "CZ";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // A marketplace with every optional group omitted — proves the mapper
    // yields empty collections and a null base currency rather than failing.
    private static final String MINIMAL_MARKETPLACE_RESPONSE = """
            {"marketplaces":[{"id":"%s"}]}
            """.formatted(MARKETPLACE_MINIMAL);
    // A defensive shape: nested language/currency/country objects with an
    // absent code. The mapper must skip them, not fail the whole listing.
    private static final String CODELESS_ENTRY_RESPONSE = """
            {"marketplaces":[{"id":"%s",
              "languages":{"offerCreation":[{"code":"%s"},{}]},
              "currencies":{"base":{"code":"%s"},"additional":[{}]},
              "shippingCountries":[{},{"code":"%s"}]}]}
            """.formatted(MARKETPLACE_PL, LANGUAGE_PL, CURRENCY_PLN, SHIPPING_COUNTRY_PL);
    // A marketplace whose currencies carry additional codes alongside the base —
    // the only shape that exercises the non-empty additionalCurrencies mapping.
    private static final String ADDITIONAL_CURRENCIES_RESPONSE = """
            {"marketplaces":[{"id":"%s",
              "currencies":{"base":{"code":"%s"},"additional":[{"code":"%s"},{"code":"%s"}]}}]}
            """.formatted(MARKETPLACE_PL, CURRENCY_PLN, CURRENCY_EUR, CURRENCY_CZK);
    // spec-derived: not yet wire-verified (errors[] contract shape; a live 404
    // capture during the bucket exploration pass will confirm or correct it)
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Resource not found",
              "userMessage":"Nie znaleziono","path":null}]}
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

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    @Test
    void list_whenMarketplacesReturned_sendsVendorAcceptAndMapsCodes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(MARKETPLACES_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Marketplace> marketplaces = allegro.marketplaces().list();

            // then — nested language/currency/country objects flattened to codes
            assertEquals(2, marketplaces.size());
            Marketplace poland = marketplaces.get(0);
            assertEquals(MARKETPLACE_PL, poland.id());
            assertEquals(CURRENCY_PLN, poland.baseCurrency());
            assertEquals(List.of(LANGUAGE_PL, LANGUAGE_EN), poland.offerCreationLanguages());
            assertEquals(List.of(LANGUAGE_PL, LANGUAGE_EN, LANGUAGE_UK), poland.offerDisplayLanguages());
            assertTrue(poland.additionalCurrencies().isEmpty());
            assertEquals(List.of(SHIPPING_COUNTRY_PL, SHIPPING_COUNTRY_CZ), poland.shippingCountries());
            verify(1, getRequestedFor(urlEqualTo(MARKETPLACES_PATH)));
        }
    }

    @Test
    void list_whenMarketplaceOmitsOptionalGroups_mapsToEmptyCollectionsAndNullBaseCurrency(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(MINIMAL_MARKETPLACE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Marketplace minimal = allegro.marketplaces().list().get(0);

            // then — no NPE on omitted groups; null base currency, empty lists
            assertEquals(MARKETPLACE_MINIMAL, minimal.id());
            assertNull(minimal.baseCurrency());
            assertTrue(minimal.offerCreationLanguages().isEmpty());
            assertTrue(minimal.offerDisplayLanguages().isEmpty());
            assertTrue(minimal.additionalCurrencies().isEmpty());
            assertTrue(minimal.shippingCountries().isEmpty());
        }
    }

    @Test
    void list_whenAdditionalCurrenciesPresent_mapsThemAlongsideBase(WireMockRuntimeInfo wmInfo) {
        // given — a marketplace with extra accepted currencies beyond the base
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ADDITIONAL_CURRENCIES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Marketplace marketplace = allegro.marketplaces().list().get(0);

            // then — the non-empty additional-currency list maps (not just the empty path)
            assertEquals(CURRENCY_PLN, marketplace.baseCurrency());
            assertEquals(List.of(CURRENCY_EUR, CURRENCY_CZK), marketplace.additionalCurrencies());
        }
    }

    @Test
    void list_whenNestedCodeMissing_skipsThatEntryWithoutFailing(WireMockRuntimeInfo wmInfo) {
        // given — a marketplace whose nested objects include a code-less entry
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CODELESS_ENTRY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Marketplace marketplace = allegro.marketplaces().list().get(0);

            // then — the code-less nested objects are dropped, not mapped to null
            assertEquals(List.of(LANGUAGE_PL), marketplace.offerCreationLanguages());
            assertEquals(CURRENCY_PLN, marketplace.baseCurrency());
            assertTrue(marketplace.additionalCurrencies().isEmpty());
            assertEquals(List.of(SHIPPING_COUNTRY_PL), marketplace.shippingCountries());
        }
    }

    @Test
    void list_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — first attempt 401, replay after re-auth carries the new token
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(MARKETPLACES_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Marketplace> marketplaces = allegro.marketplaces().list();

            // then — replayed exactly once, second request carried the fresh token
            assertEquals(2, marketplaces.size());
            verify(2, getRequestedFor(urlEqualTo(MARKETPLACES_PATH)));
            verify(1, getRequestedFor(urlEqualTo(MARKETPLACES_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void list_when401Twice_throwsAuthExceptionAfterSingleReplay(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            Marketplaces marketplaces = allegro.marketplaces();

            // then — one replay, then the typed failure with traceId
            AllegroAuthException failure =
                    assertThrows(AllegroAuthException.class, marketplaces::list);
            assertEquals(TestHttpConstants.HTTP_UNAUTHORIZED, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(2, getRequestedFor(urlEqualTo(MARKETPLACES_PATH)));
        }
    }

    @Test
    void list_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Marketplaces marketplaces = allegro.marketplaces();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, marketplaces::list);
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void list_when429Persists_retriesThenThrowsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — persistent 429 with a short Retry-After and retries enabled
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SHORT_VALUE)));
        RetryPolicy retryTwice = RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).build();

        try (AllegroClient allegro = client(wmInfo, retryTwice)) {
            Marketplaces marketplaces = allegro.marketplaces();

            // then — retried up to the attempt cap, then the typed rate-limit failure
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, marketplaces::list);
            assertEquals(RETRY_AFTER_SHORT_SECONDS, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(MARKETPLACES_PATH)));
        }
    }

    @Test
    void list_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first GET 500, second GET 200 (GETs are retried by default)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(MARKETPLACES_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(MARKETPLACES_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Marketplace> marketplaces = allegro.marketplaces().list();

            // then — the retry recovered the call
            assertEquals(2, marketplaces.size());
            verify(2, getRequestedFor(urlEqualTo(MARKETPLACES_PATH)));
        }
    }
}
