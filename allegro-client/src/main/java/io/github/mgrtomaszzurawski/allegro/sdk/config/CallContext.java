/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config;

import java.util.Objects;

/**
 * Execution metadata handed to {@link AllegroExecutionInterceptor} callbacks.
 * Metadata only — never request/response bodies or tokens.
 *
 * @param operation intent-level operation label (e.g. {@code "get current user"})
 * @param method HTTP method
 * @param path request path relative to the API base URL
 * @param attempt attempt number, starting at 1; {@code 0} in {@code beforeExecution}
 * @param statusCode HTTP status of the last response; {@code 0} when none was received
 * @param durationMillis elapsed time of the attempt/execution; {@code 0} in {@code beforeExecution}
 *
 * @since 0.1.0
 */
public record CallContext(
        String operation,
        String method,
        String path,
        int attempt,
        int statusCode,
        long durationMillis) {

    private static final String ERR_OPERATION_NULL = "operation must not be null";
    private static final String ERR_METHOD_NULL = "method must not be null";
    private static final String ERR_PATH_NULL = "path must not be null";

    public CallContext {
        Objects.requireNonNull(operation, ERR_OPERATION_NULL);
        Objects.requireNonNull(method, ERR_METHOD_NULL);
        Objects.requireNonNull(path, ERR_PATH_NULL);
    }
}
