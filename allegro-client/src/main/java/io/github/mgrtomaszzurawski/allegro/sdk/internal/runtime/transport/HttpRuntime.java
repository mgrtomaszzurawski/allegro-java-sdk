/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import java.time.Duration;

/**
 * Narrow runtime contract that {@link HttpSupport} needs from its host —
 * introduced so the low-level transport never imports the high-level
 * {@code AllegroClient} facade (layering stays one-directional).
 *
 * @since 0.1.0
 */
public interface HttpRuntime {

    /** REST API base URL for the configured environment, no trailing slash. */
    String baseUrl();

    /** Retry executor for HTTP calls. */
    RetryHandler retryHandler();

    /** Consumer-registered execution interceptor (no-op by default). */
    AllegroExecutionInterceptor executionInterceptor();

    /** Jackson mapper configured for Allegro request/response shapes. */
    ObjectMapper objectMapper();

    /** Per-request response timeout. */
    Duration readTimeout();

    /**
     * Current access token, acquired or refreshed on demand — domain clients
     * call this before every protected request, so the first call after
     * client construction never leaves with {@code Authorization: Bearer null}.
     * Cheap when the cached token is fresh.
     */
    String requireToken();

    /**
     * Drop the cached access token after an HTTP 401 so the follow-up attempt
     * re-authenticates. Single-attempt: transport retries the request once,
     * and a second 401 surfaces as {@code AllegroAuthException}.
     */
    void reauthenticate();
}
