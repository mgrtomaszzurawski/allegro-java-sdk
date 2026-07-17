/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AuthorizationCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceAuthorization;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.auth.OAuth2TokenManager;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
class OAuth2TokenManagerTest {

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_ACCESS_TOKEN = "access-token-1";
    private static final String TEST_ACCESS_TOKEN_2 = "access-token-2";
    private static final String TEST_REFRESH_TOKEN = "refresh-token-old";
    private static final String TEST_REFRESH_TOKEN_ROTATED = "refresh-token-new";
    private static final String TEST_DEVICE_CODE = "device-code-1";
    private static final String TEST_USER_CODE = "ABCD-EFGH";
    private static final String VERIFICATION_URI = "https://allegro.pl/skojarz-aplikacje";
    private static final long LONG_EXPIRY_SECONDS = 3600L;
    private static final long SHORT_EXPIRY_SECONDS = 10L;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final String GRANT_CLIENT_CREDENTIALS = "grant_type=client_credentials";
    private static final String GRANT_REFRESH = "grant_type=refresh_token";
    private static final String GRANT_DEVICE = "device_code";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","refresh_token":"%s","expires_in":%d}
            """;
    private static final String TOKEN_RESPONSE_NO_REFRESH = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String DEVICE_RESPONSE = """
            {"device_code":"%s","user_code":"%s","verification_uri":"%s",
             "verification_uri_complete":"%s?code=%s","expires_in":60,"interval":1}
            """;
    private static final String PENDING_RESPONSE = """
            {"error":"authorization_pending"}
            """;
    private static final String INVALID_GRANT_RESPONSE = """
            {"error":"invalid_grant"}
            """;
    private static final String ACCESS_DENIED_RESPONSE = """
            {"error":"access_denied"}
            """;

    private OAuth2TokenManager manager(AllegroCredentials credentials, WireMockRuntimeInfo wmInfo) {
        return new OAuth2TokenManager(credentials, wmInfo.getHttpBaseUrl() + "/auth/oauth",
                HttpClient.newHttpClient(), new ObjectMapper(), TIMEOUT);
    }

    private static ClientCredentials appCredentials() {
        return new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    }

    @Test
    void requireToken_whenClientCredentials_acquiresWithBasicAuthAndCaches(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, containing("Basic "))
                .withRequestBody(containing(GRANT_CLIENT_CREDENTIALS))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE_NO_REFRESH.formatted(TEST_ACCESS_TOKEN, LONG_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(appCredentials(), wmInfo);

        // when
        String firstToken = tokenManager.requireToken();
        String secondToken = tokenManager.requireToken();

        // then
        assertEquals(TEST_ACCESS_TOKEN, firstToken);
        assertEquals(TEST_ACCESS_TOKEN, secondToken);
        assertNull(tokenManager.currentRefreshToken());
        verify(1, postRequestedFor(urlEqualTo(TestHttpConstants.TOKEN_PATH)));
    }

    @Test
    void requireToken_whenTokenNearExpiry_refreshesProactively(WireMockRuntimeInfo wmInfo) {
        // given — expiry (10s) is inside the 60s safety margin, so the second
        // call must re-acquire even though the token has not "expired" yet
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE_NO_REFRESH.formatted(TEST_ACCESS_TOKEN, SHORT_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(appCredentials(), wmInfo);

        // when
        tokenManager.requireToken();
        tokenManager.requireToken();

        // then
        verify(2, postRequestedFor(urlEqualTo(TestHttpConstants.TOKEN_PATH)));
    }

    @Test
    void requireToken_whenStoredRefreshToken_refreshesAndRotates(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_REFRESH))
                .withRequestBody(containing(TEST_REFRESH_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(
                                TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN_ROTATED, LONG_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(AuthorizationCodeCredentials.ofRefreshToken(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_REFRESH_TOKEN), wmInfo);

        // when
        String token = tokenManager.requireToken();

        // then — rotation: the SDK now holds the NEW refresh token
        assertEquals(TEST_ACCESS_TOKEN, token);
        assertEquals(TEST_REFRESH_TOKEN_ROTATED, tokenManager.currentRefreshToken());
    }

    @Test
    void invalidate_whenCalled_nextRequireTokenReacquires(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE_NO_REFRESH.formatted(TEST_ACCESS_TOKEN, LONG_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(appCredentials(), wmInfo);
        tokenManager.requireToken();

        // when
        tokenManager.invalidate();
        tokenManager.requireToken();

        // then
        verify(2, postRequestedFor(urlEqualTo(TestHttpConstants.TOKEN_PATH)));
    }

    @Test
    void requireToken_whenRefreshRejected_fallsBackToClientCredentialsStyleGrantForDevice(
            WireMockRuntimeInfo wmInfo) {
        // given — stored device refresh token is dead; device flow restarts and succeeds
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_REFRESH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(INVALID_GRANT_RESPONSE)));
        stubFor(post(urlEqualTo(TestHttpConstants.DEVICE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEVICE_RESPONSE.formatted(TEST_DEVICE_CODE, TEST_USER_CODE,
                                VERIFICATION_URI, VERIFICATION_URI, TEST_USER_CODE))));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_DEVICE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(
                                TEST_ACCESS_TOKEN_2, TEST_REFRESH_TOKEN_ROTATED, LONG_EXPIRY_SECONDS))));
        List<DeviceAuthorization> prompts = new ArrayList<>();
        OAuth2TokenManager tokenManager = manager(DeviceCodeCredentials.ofRefreshToken(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, prompts::add, TEST_REFRESH_TOKEN), wmInfo);

        // when
        String token = tokenManager.requireToken();

        // then — user was prompted exactly once with the verification data
        assertEquals(TEST_ACCESS_TOKEN_2, token);
        assertEquals(1, prompts.size());
        assertEquals(TEST_USER_CODE, prompts.get(0).userCode());
        assertEquals(VERIFICATION_URI, prompts.get(0).verificationUri());
        verify(1, postRequestedFor(urlEqualTo(TestHttpConstants.DEVICE_PATH)));
    }

    @Test
    void requireToken_whenDevicePendingThenApproved_pollsUntilTokenIssued(WireMockRuntimeInfo wmInfo) {
        // given — first poll: authorization_pending; second poll: token
        stubFor(post(urlEqualTo(TestHttpConstants.DEVICE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEVICE_RESPONSE.formatted(TEST_DEVICE_CODE, TEST_USER_CODE,
                                VERIFICATION_URI, VERIFICATION_URI, TEST_USER_CODE))));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_DEVICE))
                .inScenario("device-poll").whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(PENDING_RESPONSE))
                .willSetStateTo("approved"));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_DEVICE))
                .inScenario("device-poll").whenScenarioStateIs("approved")
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(
                                TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN_ROTATED, LONG_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(
                DeviceCodeCredentials.of(TEST_CLIENT_ID, TEST_CLIENT_SECRET, ignored -> { }), wmInfo);

        // when
        String token = tokenManager.requireToken();

        // then — polled twice (pending, then success)
        assertEquals(TEST_ACCESS_TOKEN, token);
        verify(2, postRequestedFor(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_DEVICE)));
    }

    @Test
    void requireToken_whenAuthCodeRefreshDiesAndCodeConsumed_demandsReauthorization(
            WireMockRuntimeInfo wmInfo) {
        // given — stored refresh token rejected; an authorization-code credential
        // has no repeatable initial grant, so only the user can fix this
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(INVALID_GRANT_RESPONSE)));
        OAuth2TokenManager tokenManager = manager(AuthorizationCodeCredentials.ofRefreshToken(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_REFRESH_TOKEN), wmInfo);

        // then
        AllegroAuthException failure =
                assertThrows(AllegroAuthException.class, tokenManager::requireToken);
        assertTrue(failure.getMessage().contains("re-authorize"));
    }

    @Test
    void requireToken_whenDeviceAuthorizationDenied_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given — the user rejects the confirmation screen
        stubFor(post(urlEqualTo(TestHttpConstants.DEVICE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEVICE_RESPONSE.formatted(TEST_DEVICE_CODE, TEST_USER_CODE,
                                VERIFICATION_URI, VERIFICATION_URI, TEST_USER_CODE))));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing(GRANT_DEVICE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(ACCESS_DENIED_RESPONSE)));
        OAuth2TokenManager tokenManager = manager(
                DeviceCodeCredentials.of(TEST_CLIENT_ID, TEST_CLIENT_SECRET, ignored -> { }), wmInfo);

        // then
        AllegroAuthException failure =
                assertThrows(AllegroAuthException.class, tokenManager::requireToken);
        assertTrue(failure.responseBody().contains("access_denied"));
    }

    @Test
    void requireToken_whenTokenEndpointRejects_throwsAuthExceptionWithBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)
                        .withBody(INVALID_GRANT_RESPONSE)));
        OAuth2TokenManager tokenManager = manager(appCredentials(), wmInfo);

        // then — the server's answer survives into the exception
        AllegroAuthException failure =
                assertThrows(AllegroAuthException.class, tokenManager::requireToken);
        assertEquals(TestHttpConstants.HTTP_UNAUTHORIZED, failure.statusCode());
        assertTrue(failure.responseBody().contains("invalid_grant"));
    }

    @Test
    void requireToken_whenConcurrentCallers_singleFlightAcquiresOnce(WireMockRuntimeInfo wmInfo)
            throws InterruptedException {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withFixedDelay(200)
                        .withBody(TOKEN_RESPONSE_NO_REFRESH.formatted(TEST_ACCESS_TOKEN, LONG_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(appCredentials(), wmInfo);

        // when — two threads race the first acquisition
        Thread first = new Thread(tokenManager::requireToken);
        Thread second = new Thread(tokenManager::requireToken);
        first.start();
        second.start();
        first.join();
        second.join();

        // then — single-flight: exactly ONE token request despite two callers
        verify(1, postRequestedFor(urlEqualTo(TestHttpConstants.TOKEN_PATH)));
    }

    @Test
    void currentRefreshToken_whenAuthorizationCodeExchanged_exposesIssuedRefreshToken(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code=one-time-code"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(
                                TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN, LONG_EXPIRY_SECONDS))));
        OAuth2TokenManager tokenManager = manager(AuthorizationCodeCredentials.ofCode(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, "one-time-code", "http://localhost/callback"),
                wmInfo);

        // when
        tokenManager.requireToken();

        // then
        assertEquals(TEST_REFRESH_TOKEN, tokenManager.currentRefreshToken());
        WireMock.verify(1, postRequestedFor(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .withRequestBody(containing("redirect_uri=")));
    }
}
