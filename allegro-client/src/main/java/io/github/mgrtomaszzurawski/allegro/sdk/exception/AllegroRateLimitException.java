/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * The client hit Allegro's rate limit (HTTP 429; ~9000 requests/minute per
 * client id, plus per-user leaky buckets on some resources) — remediation:
 * slow down and retry after {@link #retryAfterSeconds()}.
 *
 * <p>Thrown only after the configured {@code RetryPolicy} exhausted its own
 * 429 retries (or when 429 retries are disabled).
 *
 * @since 0.1.0
 */
public class AllegroRateLimitException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    public AllegroRateLimitException(String message, int statusCode,
            @Nullable String responseBody, long retryAfterSeconds) {
        this(message, statusCode, responseBody, retryAfterSeconds, null);
    }

    public AllegroRateLimitException(String message, int statusCode,
            @Nullable String responseBody, long retryAfterSeconds,
            @Nullable String traceId) {
        super(message, null, statusCode, responseBody, traceId);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Server-suggested wait before the next attempt; {@code 0} if not sent. */
    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
