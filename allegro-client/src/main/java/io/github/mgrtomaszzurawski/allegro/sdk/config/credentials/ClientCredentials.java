/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;

import java.util.Objects;

/**
 * OAuth2 client-credentials grant — an application-only token with no user
 * context. Sufficient for public, non-account data (catalogue, categories,
 * public offer lookups). User-scoped resources (own offers, orders, messaging)
 * reject this token; use {@link AuthorizationCodeCredentials} or
 * {@link DeviceCodeCredentials} for those.
 *
 * @param clientId OAuth2 client id of the registered application
 * @param clientSecret OAuth2 client secret; never logged
 *
 * @since 0.1.0
 */
public record ClientCredentials(String clientId, String clientSecret) implements AllegroCredentials {

    private static final String ERR_CLIENT_ID_NULL = "clientId must not be null";
    private static final String ERR_CLIENT_SECRET_NULL = "clientSecret must not be null";

    public ClientCredentials {
        Objects.requireNonNull(clientId, ERR_CLIENT_ID_NULL);
        Objects.requireNonNull(clientSecret, ERR_CLIENT_SECRET_NULL);
    }

    /** Redacts the secret — records must never leak credentials via toString(). */
    @Override
    public String toString() {
        return "ClientCredentials[clientId=" + clientId + ", clientSecret=***]";
    }
}
