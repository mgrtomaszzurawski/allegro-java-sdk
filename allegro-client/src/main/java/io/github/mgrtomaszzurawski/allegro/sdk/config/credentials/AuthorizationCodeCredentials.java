/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 authorization-code grant — a user-context token. Two entry points:
 *
 * <ul>
 *   <li>{@link #ofCode(String, String, String, String)} — first authorization:
 *       the user has just granted access in the browser and the application
 *       received a one-time {@code code} on its {@code redirectUri}. The SDK
 *       exchanges it for an access + refresh token pair on first use.</li>
 *   <li>{@link #ofRefreshToken(String, String, String)} — returning user: a
 *       refresh token from a previous session restores access with no browser
 *       round-trip. The SDK rotates it transparently on every refresh.</li>
 * </ul>
 *
 * Exactly one of {@code authorizationCode} / {@code refreshToken} is set.
 *
 * @param clientId OAuth2 client id of the registered application
 * @param clientSecret OAuth2 client secret; never logged
 * @param authorizationCode one-time code from the browser redirect, or {@code null}
 * @param redirectUri the redirect URI the code was issued for (required with a
 *     code — Allegro validates it on exchange), or {@code null}
 * @param refreshToken refresh token from a previous session, or {@code null}
 *
 * @since 0.1.0
 */
public record AuthorizationCodeCredentials(
        String clientId,
        String clientSecret,
        @Nullable String authorizationCode,
        @Nullable String redirectUri,
        @Nullable String refreshToken) implements AllegroCredentials {

    private static final String ERR_CLIENT_ID_NULL = "clientId must not be null";
    private static final String ERR_CLIENT_SECRET_NULL = "clientSecret must not be null";
    private static final String ERR_EXACTLY_ONE_SOURCE =
            "exactly one of authorizationCode or refreshToken must be set";
    private static final String ERR_REDIRECT_URI_REQUIRED =
            "redirectUri is required with an authorizationCode (Allegro validates it on exchange)";

    public AuthorizationCodeCredentials {
        Objects.requireNonNull(clientId, ERR_CLIENT_ID_NULL);
        Objects.requireNonNull(clientSecret, ERR_CLIENT_SECRET_NULL);
        if ((authorizationCode == null) == (refreshToken == null)) {
            throw new IllegalArgumentException(ERR_EXACTLY_ONE_SOURCE);
        }
        if (authorizationCode != null && redirectUri == null) {
            throw new IllegalArgumentException(ERR_REDIRECT_URI_REQUIRED);
        }
    }

    /**
     * Credentials for the first authorization — exchanges the one-time browser
     * {@code code} for a token pair on first use.
     */
    // The "container object" PMD suggests IS this record — a factory taking the
    // four OAuth parameters directly is the clearer API here.
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static AuthorizationCodeCredentials ofCode(
            String clientId, String clientSecret, String authorizationCode, String redirectUri) {
        return new AuthorizationCodeCredentials(clientId, clientSecret,
                Objects.requireNonNull(authorizationCode, "authorizationCode must not be null"),
                Objects.requireNonNull(redirectUri, "redirectUri must not be null"),
                null);
    }

    /**
     * Credentials for a returning user — restores access from a stored refresh
     * token with no browser round-trip.
     */
    public static AuthorizationCodeCredentials ofRefreshToken(
            String clientId, String clientSecret, String refreshToken) {
        return new AuthorizationCodeCredentials(clientId, clientSecret, null, null,
                Objects.requireNonNull(refreshToken, "refreshToken must not be null"));
    }

    /** Redacts every credential material — only the client id is safe to print. */
    @Override
    public String toString() {
        return "AuthorizationCodeCredentials[clientId=" + clientId + ", clientSecret=***"
                + (authorizationCode != null ? ", authorizationCode=***" : "")
                + (refreshToken != null ? ", refreshToken=***" : "")
                + (redirectUri != null ? ", redirectUri=" + redirectUri : "")
                + "]";
    }
}
