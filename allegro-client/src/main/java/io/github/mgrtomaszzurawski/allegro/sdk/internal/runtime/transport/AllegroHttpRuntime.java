/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.auth.OAuth2TokenManager;
import java.time.Duration;

/**
 * Default {@link HttpRuntime}: glues the environment base URL, the retry
 * handler, the shared Jackson mapper, and the OAuth2 token manager together
 * for domain clients. Package-scoped state sharing is intentional — the
 * package is JPMS-non-exported.
 *
 * @since 0.1.0
 */
public final class AllegroHttpRuntime implements HttpRuntime {

    private final String baseUrl;
    private final RetryHandler retryHandler;
    private final ObjectMapper objectMapper;
    private final Duration readTimeout;
    private final OAuth2TokenManager tokenManager;
    private final AllegroExecutionInterceptor executionInterceptor;

    public AllegroHttpRuntime(String baseUrl, RetryHandler retryHandler,
            ObjectMapper objectMapper, Duration readTimeout, OAuth2TokenManager tokenManager,
            AllegroExecutionInterceptor executionInterceptor) {
        this.baseUrl = baseUrl;
        this.retryHandler = retryHandler;
        this.objectMapper = objectMapper;
        this.readTimeout = readTimeout;
        this.tokenManager = tokenManager;
        this.executionInterceptor = executionInterceptor;
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public RetryHandler retryHandler() {
        return retryHandler;
    }

    @Override
    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public AllegroExecutionInterceptor executionInterceptor() {
        return executionInterceptor;
    }

    @Override
    public String requireToken() {
        return tokenManager.requireToken();
    }

    @Override
    public void reauthenticate() {
        tokenManager.invalidate();
    }
}
