/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config;

/**
 * Allegro API environment — selects the REST API base URL and the OAuth2
 * endpoint family. The sandbox is a full functional mirror of production
 * (separate accounts, separate application registration, same rate limits).
 *
 * @since 0.1.0
 */
public enum AllegroEnvironment {

    /** Production marketplace: {@code api.allegro.pl} / {@code allegro.pl}. */
    PRODUCTION("https://api.allegro.pl", "https://allegro.pl/auth/oauth"),

    /**
     * Test environment: {@code api.allegro.pl.allegrosandbox.pl}. Applications
     * and accounts are registered separately at
     * {@code apps.developer.allegro.pl.allegrosandbox.pl}.
     */
    SANDBOX("https://api.allegro.pl.allegrosandbox.pl",
            "https://allegro.pl.allegrosandbox.pl/auth/oauth");

    private final String apiBaseUrl;
    private final String oauthBaseUrl;

    AllegroEnvironment(String apiBaseUrl, String oauthBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.oauthBaseUrl = oauthBaseUrl;
    }

    /** REST API base URL, no trailing slash. */
    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    /** OAuth2 endpoint base ({@code …/auth/oauth}), no trailing slash. */
    public String oauthBaseUrl() {
        return oauthBaseUrl;
    }

    /** OAuth2 token endpoint ({@code …/auth/oauth/token}). */
    public String tokenEndpoint() {
        return oauthBaseUrl + "/token";
    }

    /** OAuth2 device-authorization endpoint ({@code …/auth/oauth/device}). */
    public String deviceEndpoint() {
        return oauthBaseUrl + "/device";
    }

    /** OAuth2 browser authorization endpoint ({@code …/auth/oauth/authorize}). */
    public String authorizeEndpoint() {
        return oauthBaseUrl + "/authorize";
    }
}
