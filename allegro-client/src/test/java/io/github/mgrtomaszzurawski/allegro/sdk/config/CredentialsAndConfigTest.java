/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AuthorizationCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CredentialsAndConfigTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "very-secret-value";
    private static final String TEST_CODE = "one-time-code";
    private static final String TEST_REDIRECT = "http://localhost/cb";
    private static final String TEST_REFRESH = "refresh-token-value";
    private static final int INVALID_ATTEMPTS = 0;

    @Test
    void authorizationCode_whenBothCodeAndRefreshToken_rejected() {
        // then — XOR: exactly one token source
        assertThrows(IllegalArgumentException.class, () -> new AuthorizationCodeCredentials(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_CODE, TEST_REDIRECT, TEST_REFRESH));
    }

    @Test
    void authorizationCode_whenNeitherCodeNorRefreshToken_rejected() {
        // then
        assertThrows(IllegalArgumentException.class, () -> new AuthorizationCodeCredentials(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, null, null, null));
    }

    @Test
    void authorizationCode_whenCodeWithoutRedirectUri_rejected() {
        // then — Allegro validates redirect_uri on exchange; fail fast locally
        assertThrows(IllegalArgumentException.class, () -> new AuthorizationCodeCredentials(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_CODE, null, null));
    }

    @Test
    void toString_onEveryCredentialType_redactsSecretMaterial() {
        // given
        var clientCredentials = new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET);
        var codeCredentials = AuthorizationCodeCredentials.ofCode(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_CODE, TEST_REDIRECT);
        var deviceCredentials = DeviceCodeCredentials.ofRefreshToken(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, ignored -> { }, TEST_REFRESH);

        // then — no secret, code, or refresh token in any toString()
        assertFalse(clientCredentials.toString().contains(TEST_CLIENT_SECRET));
        assertFalse(codeCredentials.toString().contains(TEST_CLIENT_SECRET));
        assertFalse(codeCredentials.toString().contains(TEST_CODE));
        assertFalse(deviceCredentials.toString().contains(TEST_CLIENT_SECRET));
        assertFalse(deviceCredentials.toString().contains(TEST_REFRESH));
    }

    @Test
    void retryPolicy_whenInvalidAttempts_rejected() {
        // then
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.builder().maxAttempts(INVALID_ATTEMPTS).build());
    }

    @Test
    void retryPolicy_defaults_matchAllegroTuning() {
        // when
        RetryPolicy defaults = RetryPolicy.defaults();

        // then — the documented default contract (ADR-referenced in javadoc)
        assertEquals(3, defaults.maxAttempts());
        assertFalse(defaults.retryPost());
        assertEquals(RetryPolicy.BackoffStrategy.EXPONENTIAL, defaults.backoffStrategy());
    }

    @Test
    void clientConfig_whenNonPositiveTimeout_rejected() {
        // then
        assertThrows(IllegalArgumentException.class,
                () -> AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .readTimeout(Duration.ZERO).build());
    }

    @Test
    void clientConfig_defaults_deriveBaseUrlsFromEnvironment() {
        // when
        AllegroClientConfig config = AllegroClientConfig.defaults(AllegroEnvironment.SANDBOX);

        // then
        assertEquals(AllegroEnvironment.SANDBOX.apiBaseUrl(), config.apiBaseUrl());
        assertEquals(AllegroEnvironment.SANDBOX.oauthBaseUrl(), config.oauthBaseUrl());
    }
}
