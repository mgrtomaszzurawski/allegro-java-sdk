/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;

/**
 * Application credentials for one of the three OAuth2 grants Allegro supports.
 * The consumer picks the grant by choosing the implementation; everything that
 * follows — token acquisition, caching, proactive refresh, refresh-token
 * rotation, and the single-attempt re-auth on HTTP 401 — is SDK-internal.
 *
 * <ul>
 *   <li>{@link ClientCredentials} — application-only token (no user context);
 *       enough for public data such as catalogue reads.</li>
 *   <li>{@link AuthorizationCodeCredentials} — user token from the browser
 *       authorization-code flow (or directly from a stored refresh token).</li>
 *   <li>{@link DeviceCodeCredentials} — user token for console/headless
 *       applications; the user confirms once at a verification URI.</li>
 * </ul>
 *
 * @since 0.1.0
 */
public sealed interface AllegroCredentials
        permits ClientCredentials, AuthorizationCodeCredentials, DeviceCodeCredentials {

    /** OAuth2 client id of the registered Allegro application. */
    String clientId();

    /** OAuth2 client secret of the registered Allegro application. Never logged. */
    String clientSecret();
}
