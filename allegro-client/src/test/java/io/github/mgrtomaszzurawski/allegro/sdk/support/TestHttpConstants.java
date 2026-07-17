/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.support;

/**
 * Shared HTTP literals for tests — single source so stubs and assertions
 * never drift apart on header names or media types.
 */
public final class TestHttpConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String VND_ALLEGRO_V1 = "application/vnd.allegro.public.v1+json";
    public static final String APPLICATION_JSON = "application/json";
    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String TRACE_ID_HEADER = "trace-id";
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    public static final int HTTP_SERVER_ERROR = 500;

    public static final String TOKEN_PATH = "/auth/oauth/token";
    public static final String DEVICE_PATH = "/auth/oauth/device";

    private TestHttpConstants() {
    }
}
