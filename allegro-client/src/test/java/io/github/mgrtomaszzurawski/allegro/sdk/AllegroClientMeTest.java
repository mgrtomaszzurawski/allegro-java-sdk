/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk;

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
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * End-to-end proof of the whole stack against WireMock: OAuth2 token dance →
 * transport with vendor media type → 401 replay → typed error mapping →
 * Raw → domain-record mapping.
 */
@WireMockTest
class AllegroClientMeTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String ME_PATH = "/me";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // Wire-verified shape (sandbox 2026-07-17): company fields arrive as plain
    // JSON but the generated DTO wraps them in JsonNullable — deserialization
    // fails unless the SDK mapper registers JsonNullableModule (regression).
    private static final String ME_RESPONSE = """
            {"id":"123","login":"seller-login","firstName":"Jan","lastName":"Tester",
             "email":"seller@example.com","features":["feature-a"],
             "company":{"name":"test"},"baseMarketplace":{"id":"allegro-pl"}}
            """;
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFoundException","message":"User not found",
              "userMessage":"Nie znaleziono","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    @Test
    void me_whenAuthenticated_sendsVendorHeadersAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ME_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ME_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            CurrentUser currentUser = allegro.user().me();

            // then — Raw mapped to the immutable domain record
            assertEquals("123", currentUser.id());
            assertEquals("seller-login", currentUser.login());
            assertEquals("seller@example.com", currentUser.email());
            assertEquals(1, currentUser.features().size());
            verify(1, getRequestedFor(urlEqualTo(ME_PATH)));
        }
    }

    @Test
    void me_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — first /me: 401; after re-auth: 200. Token endpoint hands out
        // token-one first, token-two on the second acquisition.
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(ME_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(ME_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ME_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            CurrentUser currentUser = allegro.user().me();

            // then — replayed exactly once, second request carried the FRESH token
            assertEquals("123", currentUser.id());
            verify(2, getRequestedFor(urlEqualTo(ME_PATH)));
            verify(1, getRequestedFor(urlEqualTo(ME_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void me_when401Twice_throwsAuthExceptionAfterSingleReplay(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ME_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var userAccount = allegro.user();

            // then — exactly one replay, then the typed failure with traceId
            AllegroAuthException failure =
                    assertThrows(AllegroAuthException.class, userAccount::me);
            assertEquals(TestHttpConstants.HTTP_UNAUTHORIZED, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(2, getRequestedFor(urlEqualTo(ME_PATH)));
        }
    }

    @Test
    void me_when404_throwsNotFoundWithParsedBodyAndTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ME_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var userAccount = allegro.user();

            // then — the server's answer survives: status, body, trace-id
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, userAccount::me);
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertTrue(failure.responseBody().contains("NotFoundException"));
        }
    }

    @Test
    void sdkVersion_whenRunFromClassesDirectory_reportsUnversioned() {
        // then — no JAR manifest and no module-descriptor version in test runs
        assertEquals("unversioned", AllegroClient.sdkVersion());
    }

    @Test
    void resolveVersion_prefersModuleDescriptorThenManifestThenFallback() {
        // given
        var versionedDescriptor = java.lang.module.ModuleDescriptor
                .newModule("synthetic.module").version("9.9.9").build();
        var unversionedDescriptor = java.lang.module.ModuleDescriptor
                .newModule("synthetic.module").build();

        // then — descriptor wins, manifest is the fallback, sentinel last
        assertEquals("9.9.9", AllegroClient.resolveVersion(versionedDescriptor, "1.1.1"));
        assertEquals("1.1.1", AllegroClient.resolveVersion(unversionedDescriptor, "1.1.1"));
        assertEquals("1.1.1", AllegroClient.resolveVersion(null, "1.1.1"));
        assertEquals("unversioned", AllegroClient.resolveVersion(null, null));
    }

    @Test
    void refreshToken_whenClientCredentialsGrant_returnsNull(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ME_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ME_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            allegro.user().me();

            // then — app-only grant issues no refresh token
            assertNull(allegro.refreshToken());
        }
    }

    @Test
    void user_whenClientClosed_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given
        AllegroClient allegro = client(wmInfo);
        allegro.close();

        // then
        assertThrows(IllegalStateException.class, allegro::user);
    }
}
