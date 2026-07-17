/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config;

import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;

/**
 * Hook around every SDK HTTP execution — the analogue of AWS SDK v2's
 * {@code ExecutionInterceptor}. Register one via
 * {@code AllegroClientConfig.builder(...).executionInterceptor(...)}; all
 * methods default to no-ops, implement only what you need. Consumers
 * typically build metrics (Micrometer/OTel) on this seam.
 *
 * <p>Contract: callbacks run synchronously on the calling thread — keep them
 * fast and never throw (a throwing interceptor fails the SDK call). Failure
 * callbacks receive the typed {@link AllegroException}, which already carries
 * the server's status, error payload, and {@code trace-id}; the interceptor
 * never sees request/response bodies.
 *
 * @since 0.1.0
 */
public interface AllegroExecutionInterceptor {

    /** Called once before the first attempt of an execution. */
    default void beforeExecution(CallContext context) {
    }

    /** Called after EVERY attempt (including retried ones), success or not. */
    default void afterAttempt(CallContext context) {
    }

    /** Called once after the execution completed successfully (2xx). */
    default void afterExecution(CallContext context) {
    }

    /** Called once when the execution failed (typed exception is being thrown). */
    default void onExecutionFailure(CallContext context, AllegroException failure) {
    }

    /** Interceptor that does nothing — the default. */
    static AllegroExecutionInterceptor noop() {
        return new AllegroExecutionInterceptor() {
        };
    }
}
