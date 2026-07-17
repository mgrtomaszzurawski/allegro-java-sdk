/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.CallContext;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes HTTP requests with the configured {@link RetryPolicy}: retries on
 * 5xx and network failures ({@code retryOn5xx}) and on 429
 * ({@code retryOn429}, honouring {@code Retry-After} capped at
 * {@code maxRetryAfterSeconds}). POST requests are retried only when
 * {@code retryPost} is set — most Allegro write commands are not idempotent.
 *
 * <p>Backoff uses <em>equal jitter</em>: the actual sleep is drawn uniformly
 * from {@code [base/2, base]}, where {@code base} doubles per attempt under
 * {@code EXPONENTIAL} and stays constant under {@code FIXED}. Jitter prevents
 * synchronized retry storms across client instances.
 *
 * @since 0.1.0
 */
public final class RetryHandler {

    private static final long BASE_BACKOFF_MILLIS = 500L;
    /** Exponent clamp: 500ms * 2^6 = 32s max exponential base (also guards << overflow). */
    private static final int MAX_BACKOFF_EXPONENT = 6;
    private static final long MAX_BACKOFF_MILLIS = 60_000L;
    private static final String WARN_INTERCEPTOR = "execution interceptor threw - ignored: {}";
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MIN = 500;
    private static final String POST_METHOD = "POST";
    private static final String LOG_ATTEMPT = "{}: {} {} -> {} (attempt {}/{}, {} ms)";
    private static final String LOG_BACKOFF = "{}: retrying in {} ms (attempt {}/{})";
    private static final String ERR_NETWORK = "Network failure calling Allegro";
    private static final String ERR_INTERRUPTED = "Interrupted while calling Allegro";

    private final HttpClient httpClient;
    private final RetryPolicy policy;

    public RetryHandler(HttpClient httpClient, RetryPolicy policy) {
        this.httpClient = httpClient;
        this.policy = policy;
    }

    /**
     * Send the request, retrying per policy. Returns the last response —
     * including non-2xx ones — so the caller maps status codes to typed
     * exceptions; throws {@link AllegroServerException} only when the network
     * itself failed on the final attempt.
     */
    public HttpResponse<String> send(HttpRequest request, String operationName,
            AllegroExecutionInterceptor interceptor) {
        int attemptsAllowed = policy.enabled() ? policy.maxAttempts() : 1;
        boolean retryableMethod = policy.retryPost() || !POST_METHOD.equals(request.method());
        String path = request.uri().getPath();
        IOException lastNetworkFailure = null;
        for (int attempt = 1; attempt <= attemptsAllowed; attempt++) {
            boolean lastAttempt = attempt == attemptsAllowed;
            long attemptStart = System.nanoTime();
            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                recordAttempt(interceptor, attemptsAllowed, new CallContext(operationName,
                        request.method(), path, attempt, response.statusCode(),
                        elapsedMillis(attemptStart)));
                if (lastAttempt || !retryableMethod || !isRetryableStatus(response.statusCode())) {
                    return response;
                }
                long backoff = backoffMillis(attempt, retryAfterMillis(response));
                SdkLoggers.RETRY.warn(LOG_BACKOFF, operationName, backoff, attempt, attemptsAllowed);
                sleep(backoff);
            } catch (IOException e) {
                lastNetworkFailure = e;
                recordAttempt(interceptor, attemptsAllowed, new CallContext(operationName,
                        request.method(), path, attempt, 0, elapsedMillis(attemptStart)));
                if (lastAttempt || !retryableMethod || !policy.retryOn5xx()) {
                    throw new AllegroServerException(ERR_NETWORK, e);
                }
                long backoff = backoffMillis(attempt, 0L);
                SdkLoggers.RETRY.warn(LOG_BACKOFF, operationName, backoff, attempt, attemptsAllowed);
                sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AllegroServerException(ERR_INTERRUPTED, e);
            }
        }
        // Unreachable: the loop always returns or throws on the last attempt.
        throw new AllegroServerException(ERR_NETWORK, lastNetworkFailure);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static void recordAttempt(AllegroExecutionInterceptor interceptor,
            int attemptsAllowed, CallContext context) {
        try {
            interceptor.afterAttempt(context);
        } catch (RuntimeException e) {
            // The SPI contract says callbacks must not throw; a misbehaving
            // consumer hook must not abort the retry loop it observes.
            SdkLoggers.RETRY.warn(WARN_INTERCEPTOR, e.toString());
        }
        if (SdkLoggers.RETRY.isDebugEnabled()) {
            SdkLoggers.RETRY.debug(LOG_ATTEMPT, context.operation(), context.method(),
                    context.path(), context.statusCode(), context.attempt(), attemptsAllowed,
                    context.durationMillis());
        }
    }

    private boolean isRetryableStatus(int status) {
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return policy.retryOn429();
        }
        return status >= HTTP_SERVER_ERROR_MIN && policy.retryOn5xx();
    }

    private long retryAfterMillis(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_TOO_MANY_REQUESTS) {
            return 0L;
        }
        long retryAfterSeconds = Math.min(
                ServerErrorParser.parseRetryAfterSeconds(response), policy.maxRetryAfterSeconds());
        return retryAfterSeconds * MILLIS_PER_SECOND;
    }

    /**
     * Equal-jitter backoff: the base doubles per attempt (EXPONENTIAL, clamped
     * exponent and 60s cap) or stays flat (FIXED); the actual sleep is uniform
     * in {@code [base/2, base]}. A server-sent {@code Retry-After} is a FLOOR
     * — jitter never dips below the server-mandated wait.
     */
    private long backoffMillis(int attempt, long retryAfterMillis) {
        long base = policy.backoffStrategy() == RetryPolicy.BackoffStrategy.EXPONENTIAL
                ? BASE_BACKOFF_MILLIS * (1L << Math.min(attempt - 1, MAX_BACKOFF_EXPONENT))
                : BASE_BACKOFF_MILLIS;
        base = Math.min(base, MAX_BACKOFF_MILLIS);
        long half = base / 2;
        long jittered = half + ThreadLocalRandom.current().nextLong(base - half + 1);
        return Math.max(jittered, retryAfterMillis);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AllegroServerException(ERR_INTERRUPTED, e);
        }
    }
}
