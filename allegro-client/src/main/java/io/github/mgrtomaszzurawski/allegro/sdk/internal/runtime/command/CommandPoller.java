/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command;

import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.SdkLoggers;
import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Turns Allegro's asynchronous command endpoints into synchronous SDK calls
 * (ADR-005, ARCHITECTURE §8). Many write operations — batch offer modification,
 * offer create/edit, badge and subsidy commands, fulfillment ASN submission —
 * return a command/operation handle and complete later; the consumer surface
 * must not expose that. A domain client submits the command, then hands this
 * poller two lambdas: how to <em>read</em> the current status and how to decide
 * it reached a <em>terminal</em> state. The poller owns the wait loop, the
 * bounded backoff, the deadline, and the timeout exception; it knows nothing
 * about the feature-specific status shape.
 *
 * <p>No {@code CompletableFuture} leaks into the public API: {@link #await} blocks
 * the calling thread until the command resolves or the deadline passes, at which
 * point it throws {@link AllegroAsyncTimeoutException} (the command may still be
 * completing server-side — the caller re-reads the resource, never blindly
 * resubmits).
 *
 * <p>Stateless and thread-safe; one instance may back many concurrent commands.
 *
 * @since 0.2.0
 */
public final class CommandPoller {

    /** Sleep abstraction so tests advance time without real waiting. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private static final long DEFAULT_BASE_DELAY_MILLIS = 500L;
    private static final long DEFAULT_MAX_DELAY_MILLIS = 5_000L;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    /** Cap the shift so {@code base << n} cannot overflow or explode past the delay cap. */
    private static final int MAX_BACKOFF_SHIFT = 6;

    private static final String LOG_POLLING = "{}: command not terminal after attempt {}, waiting {} ms";
    private static final String ERR_TIMEOUT =
            "Timed out after %s waiting for '%s' to reach a terminal state - the command may "
                    + "still be completing; re-read the resource before resubmitting";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting for an Allegro command";

    private final Sleeper sleeper;
    private final LongSupplier nanoClock;
    private final long baseDelayMillis;
    private final long maxDelayMillis;
    private final Duration defaultTimeout;

    /** Production poller: real sleep, real clock, default backoff and timeout. */
    public CommandPoller() {
        this(Thread::sleep, System::nanoTime, DEFAULT_BASE_DELAY_MILLIS, DEFAULT_MAX_DELAY_MILLIS,
                DEFAULT_TIMEOUT);
    }

    /** Wiring/test constructor with an injectable sleeper, clock and backoff. */
    public CommandPoller(Sleeper sleeper, LongSupplier nanoClock, long baseDelayMillis,
            long maxDelayMillis, Duration defaultTimeout) {
        this.sleeper = sleeper;
        this.nanoClock = nanoClock;
        this.baseDelayMillis = baseDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.defaultTimeout = defaultTimeout;
    }

    /**
     * Poll to a terminal state using the default timeout.
     *
     * @see #await(Supplier, Predicate, String, Duration)
     */
    public <S> S await(Supplier<S> fetchStatus, Predicate<S> isTerminal, String operationName) {
        return await(fetchStatus, isTerminal, operationName, defaultTimeout);
    }

    /**
     * Read the status, and while it is non-terminal wait with bounded backoff and
     * read again, until {@code isTerminal} holds or {@code timeout} elapses.
     *
     * @param fetchStatus   reads the current command status (one HTTP GET per call)
     * @param isTerminal    {@code true} once the status needs no more polling
     * @param operationName human-readable label for logs and the timeout message
     * @param timeout       overall budget across all attempts
     * @param <S>           feature-specific status type
     * @return the first status for which {@code isTerminal} returned {@code true}
     * @throws AllegroAsyncTimeoutException if the deadline passes first
     */
    public <S> S await(Supplier<S> fetchStatus, Predicate<S> isTerminal, String operationName,
            Duration timeout) {
        long deadlineNanos = nanoClock.getAsLong() + timeout.toNanos();
        int attempt = 0;
        while (true) {
            S status = fetchStatus.get();
            if (isTerminal.test(status)) {
                return status;
            }
            attempt++;
            long remainingMillis = remainingMillis(deadlineNanos);
            if (remainingMillis <= 0) {
                throw new AllegroAsyncTimeoutException(
                        ERR_TIMEOUT.formatted(timeout, operationName));
            }
            long delayMillis = Math.min(backoffMillis(attempt), remainingMillis);
            SdkLoggers.RETRY.debug(LOG_POLLING, operationName, attempt, delayMillis);
            sleep(delayMillis);
        }
    }

    private long remainingMillis(long deadlineNanos) {
        return (deadlineNanos - nanoClock.getAsLong()) / 1_000_000L;
    }

    private long backoffMillis(int attempt) {
        long scaled = baseDelayMillis * (1L << Math.min(attempt - 1, MAX_BACKOFF_SHIFT));
        return Math.min(scaled, maxDelayMillis);
    }

    private void sleep(long delayMillis) {
        try {
            sleeper.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AllegroServerException(ERR_INTERRUPTED, e);
        }
    }
}
