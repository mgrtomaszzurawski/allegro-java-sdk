/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AuthorizationCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceAuthorization;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.SdkLoggers;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.jspecify.annotations.Nullable;

/**
 * Owns the whole OAuth2 lifecycle so the consumer authenticates exactly once:
 * lazy first acquisition, in-memory caching, <strong>proactive refresh</strong>
 * before expiry (safety margin), refresh-token <strong>rotation</strong>, the
 * device-flow verification dance, and re-acquisition after
 * {@link #invalidate()} (the transport's single-attempt 401 recovery hook).
 *
 * <p>Thread-safe: acquisition/refresh is single-flight — concurrent callers
 * block on one refresh instead of stampeding the token endpoint.
 *
 * <p>Never logs token material; failures carry
 * {@code AllegroException.safeResponseBody()}-redacted bodies only.
 *
 * @since 0.1.0
 */
public final class OAuth2TokenManager {

    /** Refresh this long before the token's nominal expiry. */
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60);
    /** Fallback lifetime when the token response carries no {@code expires_in}. */
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;
    /** Device-flow fallback poll interval when the server sends none. */
    private static final long DEFAULT_DEVICE_POLL_SECONDS = 5L;
    /** RFC 8628 back-off bump applied on {@code slow_down}. */
    private static final long SLOW_DOWN_INCREMENT_SECONDS = 5L;

    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    private static final String GRANT_CLIENT_CREDENTIALS = "grant_type=client_credentials";
    private static final String GRANT_AUTHORIZATION_CODE = "grant_type=authorization_code";
    private static final String GRANT_REFRESH_TOKEN = "grant_type=refresh_token";
    private static final String GRANT_DEVICE_CODE =
            "grant_type=urn:ietf:params:oauth:grant-type:device_code";
    private static final String PARAM_CODE = "code";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";
    private static final String PARAM_REFRESH_TOKEN = "refresh_token";
    private static final String PARAM_DEVICE_CODE = "device_code";
    private static final String PARAM_CLIENT_ID = "client_id";

    private static final String FIELD_ACCESS_TOKEN = "access_token";
    private static final String FIELD_REFRESH_TOKEN = "refresh_token";
    private static final String FIELD_EXPIRES_IN = "expires_in";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_DEVICE_CODE = "device_code";
    private static final String FIELD_USER_CODE = "user_code";
    private static final String FIELD_VERIFICATION_URI = "verification_uri";
    private static final String FIELD_VERIFICATION_URI_COMPLETE = "verification_uri_complete";
    private static final String FIELD_INTERVAL = "interval";

    private static final String ERROR_AUTHORIZATION_PENDING = "authorization_pending";
    private static final String ERROR_SLOW_DOWN = "slow_down";

    private static final String ERR_TOKEN_ENDPOINT = "OAuth2 token endpoint rejected the request";
    private static final String ERR_DEVICE_ENDPOINT = "OAuth2 device endpoint rejected the request";
    private static final String ERR_DEVICE_DENIED = "Device-flow authorization was denied or expired";
    private static final String ERR_DEVICE_TIMEOUT = "Device-flow authorization timed out";
    private static final String ERR_NO_ACCESS_TOKEN = "Token response carries no access_token";
    private static final String ERR_NETWORK = "Network failure calling the OAuth2 endpoint";
    private static final String ERR_INTERRUPTED = "Interrupted during OAuth2 token acquisition";
    private static final String ERR_CODE_CONSUMED =
            "Refresh failed and the one-time authorization code was already used - re-authorize the user";

    private static final String LOG_REFRESHING = "refreshing access token";
    private static final String LOG_REFRESH_REJECTED = "stored refresh token rejected - falling back to initial grant";
    private static final String LOG_INITIAL_GRANT = "acquiring token via {}";
    private static final String LOG_DEVICE_STARTED = "device flow started - waiting for user confirmation (poll every {} s)";
    private static final String LOG_DEVICE_PENDING = "device authorization pending";
    private static final String LOG_TOKEN_STORED = "access token acquired (expires in {} s, refresh token {})";
    private static final String LOG_INVALIDATED = "access token invalidated - next call re-acquires";
    private static final String GRANT_LABEL_CLIENT = "client_credentials";
    private static final String GRANT_LABEL_CODE = "authorization_code";
    private static final String REFRESH_PRESENT = "rotated";
    private static final String REFRESH_ABSENT = "absent";

    private final AllegroCredentials credentials;
    private final AllegroEnvironment environment;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration readTimeout;
    private final Object refreshLock = new Object();

    private volatile @Nullable String accessToken;
    private volatile Instant expiresAt = Instant.EPOCH;
    private volatile @Nullable String refreshToken;
    private volatile boolean initialGrantConsumed;

    public OAuth2TokenManager(AllegroCredentials credentials, AllegroEnvironment environment,
            HttpClient httpClient, ObjectMapper objectMapper, Duration readTimeout) {
        this.credentials = credentials;
        this.environment = environment;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.readTimeout = readTimeout;
        this.refreshToken = storedRefreshToken(credentials);
    }

    /**
     * Current access token, acquiring or refreshing first when the cache is
     * empty or within the expiry margin. Cheap when the cached token is fresh.
     */
    public String requireToken() {
        String cached = accessToken;
        if (cached != null && Instant.now().isBefore(expiresAt.minus(EXPIRY_MARGIN))) {
            return cached;
        }
        synchronized (refreshLock) {
            cached = accessToken;
            if (cached != null && Instant.now().isBefore(expiresAt.minus(EXPIRY_MARGIN))) {
                return cached;
            }
            acquireOrRefresh();
            return accessToken;
        }
    }

    /**
     * Drop the cached access token so the next {@link #requireToken()}
     * re-acquires — the transport's single-attempt recovery hook for HTTP 401.
     */
    public void invalidate() {
        SdkLoggers.AUTH.debug(LOG_INVALIDATED);
        accessToken = null;
    }

    /**
     * Refresh token currently held (rotated on every refresh), or {@code null}
     * for app-only credentials. Consumers persist it between sessions via the
     * client facade to skip future browser/device prompts.
     */
    public @Nullable String currentRefreshToken() {
        return refreshToken;
    }

    private void acquireOrRefresh() {
        String currentRefresh = refreshToken;
        if (currentRefresh != null) {
            try {
                SdkLoggers.AUTH.debug(LOG_REFRESHING);
                storeTokens(tokenRequest(GRANT_REFRESH_TOKEN
                        + '&' + PARAM_REFRESH_TOKEN + '=' + urlEncode(currentRefresh)));
                return;
            } catch (AllegroAuthException e) {
                // Stored/rotated refresh token revoked or expired — fall through
                // to a fresh initial grant where the credential type allows one.
                SdkLoggers.AUTH.warn(LOG_REFRESH_REJECTED);
                refreshToken = null;
            }
        }
        initialAcquire();
    }

    private void initialAcquire() {
        // Sealed hierarchy walked with instanceof patterns — switch patterns
        // are preview-only on the Java 17 baseline.
        if (credentials instanceof AuthorizationCodeCredentials authorizationCode) {
            SdkLoggers.AUTH.debug(LOG_INITIAL_GRANT, GRANT_LABEL_CODE);
            initialAuthorizationCode(authorizationCode);
        } else if (credentials instanceof DeviceCodeCredentials deviceCode) {
            deviceFlow(deviceCode);
        } else {
            SdkLoggers.AUTH.debug(LOG_INITIAL_GRANT, GRANT_LABEL_CLIENT);
            storeTokens(tokenRequest(GRANT_CLIENT_CREDENTIALS));
        }
    }

    private void initialAuthorizationCode(AuthorizationCodeCredentials authorizationCode) {
        String oneTimeCode = authorizationCode.authorizationCode();
        if (oneTimeCode == null || initialGrantConsumed) {
            // Constructed from a refresh token that no longer works, or the
            // one-time code was already exchanged — only the user can fix this.
            throw new AllegroAuthException(ERR_CODE_CONSUMED, null);
        }
        initialGrantConsumed = true;
        storeTokens(tokenRequest(GRANT_AUTHORIZATION_CODE
                + '&' + PARAM_CODE + '=' + urlEncode(oneTimeCode)
                + '&' + PARAM_REDIRECT_URI + '=' + urlEncode(authorizationCode.redirectUri())));
    }

    // ---- device flow (RFC 8628) ----

    private void deviceFlow(DeviceCodeCredentials deviceCredentials) {
        JsonNode deviceResponse = postForm(environment.deviceEndpoint(),
                PARAM_CLIENT_ID + '=' + urlEncode(credentials.clientId()), ERR_DEVICE_ENDPOINT);
        long expiresInSeconds = deviceResponse.path(FIELD_EXPIRES_IN)
                .asLong(DEFAULT_EXPIRES_IN_SECONDS);
        long pollSeconds = deviceResponse.path(FIELD_INTERVAL).asLong(DEFAULT_DEVICE_POLL_SECONDS);
        String deviceCode = deviceResponse.path(FIELD_DEVICE_CODE).asText();

        deviceCredentials.userPrompt().accept(new DeviceAuthorization(
                deviceResponse.path(FIELD_USER_CODE).asText(),
                deviceResponse.path(FIELD_VERIFICATION_URI).asText(),
                deviceResponse.path(FIELD_VERIFICATION_URI_COMPLETE).asText(),
                Duration.ofSeconds(expiresInSeconds)));

        SdkLoggers.AUTH.debug(LOG_DEVICE_STARTED, pollSeconds);
        Instant deadline = Instant.now().plusSeconds(expiresInSeconds);
        String pollForm = GRANT_DEVICE_CODE + '&' + PARAM_DEVICE_CODE + '=' + urlEncode(deviceCode);
        while (Instant.now().isBefore(deadline)) {
            sleepSeconds(pollSeconds);
            HttpResponse<String> response = send(tokenEndpointRequest(
                    environment.tokenEndpoint(), pollForm));
            if (response.statusCode() == HTTP_OK) {
                storeTokens(parseJson(response.body()));
                return;
            }
            String oauthError = parseJson(response.body()).path(FIELD_ERROR).asText();
            if (response.statusCode() == HTTP_BAD_REQUEST
                    && ERROR_AUTHORIZATION_PENDING.equals(oauthError)) {
                SdkLoggers.AUTH.debug(LOG_DEVICE_PENDING);
                continue;
            }
            if (response.statusCode() == HTTP_BAD_REQUEST && ERROR_SLOW_DOWN.equals(oauthError)) {
                pollSeconds += SLOW_DOWN_INCREMENT_SECONDS;
                continue;
            }
            throw new AllegroAuthException(ERR_DEVICE_DENIED, response.statusCode(), response.body());
        }
        throw new AllegroAuthException(ERR_DEVICE_TIMEOUT, null);
    }

    // ---- token endpoint plumbing ----

    private JsonNode tokenRequest(String formBody) {
        return postForm(environment.tokenEndpoint(), formBody, ERR_TOKEN_ENDPOINT);
    }

    private JsonNode postForm(String endpoint, String formBody, String failureMessage) {
        HttpResponse<String> response = send(tokenEndpointRequest(endpoint, formBody));
        if (response.statusCode() != HTTP_OK) {
            throw new AllegroAuthException(failureMessage, response.statusCode(), response.body());
        }
        return parseJson(response.body());
    }

    private HttpRequest tokenEndpointRequest(String endpoint, String formBody) {
        return HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(readTimeout)
                .header(CONTENT_TYPE_HEADER, FORM_CONTENT_TYPE)
                .header(AUTHORIZATION_HEADER, basicAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AllegroServerException(ERR_NETWORK, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AllegroServerException(ERR_INTERRUPTED, e);
        }
    }

    private void storeTokens(JsonNode tokenResponse) {
        JsonNode tokenNode = tokenResponse.path(FIELD_ACCESS_TOKEN);
        if (tokenNode.isMissingNode() || tokenNode.asText().isEmpty()) {
            throw new AllegroAuthException(ERR_NO_ACCESS_TOKEN, null);
        }
        // Rotation: Allegro issues a NEW refresh token on every refresh; the
        // old one is dead the moment this response arrives.
        JsonNode rotatedRefresh = tokenResponse.path(FIELD_REFRESH_TOKEN);
        if (!rotatedRefresh.isMissingNode() && !rotatedRefresh.asText().isEmpty()) {
            refreshToken = rotatedRefresh.asText();
        }
        long expiresInSeconds = tokenResponse.path(FIELD_EXPIRES_IN).asLong(DEFAULT_EXPIRES_IN_SECONDS);
        expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        accessToken = tokenNode.asText();
        SdkLoggers.AUTH.debug(LOG_TOKEN_STORED, expiresInSeconds,
                refreshToken != null ? REFRESH_PRESENT : REFRESH_ABSENT);
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new AllegroAuthException(ERR_TOKEN_ENDPOINT, e);
        }
    }

    private String basicAuthHeader() {
        String pair = credentials.clientId() + ':' + credentials.clientSecret();
        return BASIC_PREFIX + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    private static @Nullable String storedRefreshToken(AllegroCredentials credentials) {
        if (credentials instanceof AuthorizationCodeCredentials authorizationCode) {
            return authorizationCode.refreshToken();
        }
        if (credentials instanceof DeviceCodeCredentials deviceCode) {
            return deviceCode.refreshToken();
        }
        return null;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AllegroServerException(ERR_INTERRUPTED, e);
        }
    }
}
