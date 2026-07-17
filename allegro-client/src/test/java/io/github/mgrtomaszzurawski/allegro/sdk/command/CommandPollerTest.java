/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class CommandPollerTest {

    private static final long BASE_DELAY_MILLIS = 500L;
    private static final long MAX_DELAY_MILLIS = 5_000L;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(2);
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final String OPERATION = "publish offers";
    private static final int TERMINAL_AFTER_THREE = 3;

    /** Fake clock/sleeper: sleeping advances virtual time, so no test really waits. */
    private static final class VirtualTime {
        private long nowNanos;
        private final List<Long> sleptMillis = new ArrayList<>();

        LongSupplier clock() {
            return () -> nowNanos;
        }

        CommandPoller.Sleeper sleeper() {
            return millis -> {
                sleptMillis.add(millis);
                nowNanos += millis * NANOS_PER_MILLI;
            };
        }
    }

    private static CommandPoller poller(VirtualTime time) {
        return new CommandPoller(time.sleeper(), time.clock(),
                BASE_DELAY_MILLIS, MAX_DELAY_MILLIS, TIMEOUT);
    }

    @Test
    void await_whenTerminalOnFirstPoll_returnsWithoutSleeping() {
        // given — the command is already done at the first status read
        VirtualTime time = new VirtualTime();
        AtomicInteger polls = new AtomicInteger();

        // when
        Integer result = poller(time).await(
                polls::incrementAndGet, status -> status >= 1, OPERATION);

        // then — exactly one status read, zero waits
        assertEquals(1, result);
        assertEquals(1, polls.get());
        assertTrue(time.sleptMillis.isEmpty());
    }

    @Test
    void await_whenTerminalAfterSeveralPolls_returnsTerminalStatusWithGrowingBackoff() {
        // given — terminal only on the third read
        VirtualTime time = new VirtualTime();
        AtomicInteger polls = new AtomicInteger();

        // when
        Integer result = poller(time).await(
                polls::incrementAndGet, status -> status >= TERMINAL_AFTER_THREE, OPERATION);

        // then — three reads, two waits, backoff doubled (500, 1000)
        assertEquals(TERMINAL_AFTER_THREE, result);
        assertEquals(TERMINAL_AFTER_THREE, polls.get());
        assertEquals(List.of(BASE_DELAY_MILLIS, BASE_DELAY_MILLIS * 2), time.sleptMillis);
    }

    @Test
    void await_whenNeverTerminal_throwsAsyncTimeoutAfterDeadline() {
        // given — status stays non-terminal; the short deadline is 2 s
        VirtualTime time = new VirtualTime();

        // then
        AllegroAsyncTimeoutException failure = assertThrows(AllegroAsyncTimeoutException.class,
                () -> poller(time).await(() -> 0, status -> false, OPERATION, SHORT_TIMEOUT));
        assertTrue(failure.getMessage().contains(OPERATION));
    }

    @Test
    void await_whenApproachingDeadline_lastWaitDoesNotOversleepPastIt() {
        // given — 2 s budget: waits 500 then 1000 (1.5 s), the third wait must be
        // clamped to the remaining 500 ms rather than the 2000 ms backoff step
        VirtualTime time = new VirtualTime();

        // when
        assertThrows(AllegroAsyncTimeoutException.class,
                () -> poller(time).await(() -> 0, status -> false, OPERATION, SHORT_TIMEOUT));

        // then — no single sleep exceeded the remaining budget
        assertEquals(List.of(500L, 1000L, 500L), time.sleptMillis);
    }

    @Test
    void await_whenBackoffScales_isCappedAtMaxDelay() {
        // given — a long deadline so backoff can climb to the cap; terminal on read 10
        VirtualTime time = new VirtualTime();
        AtomicInteger polls = new AtomicInteger();

        // when
        poller(time).await(polls::incrementAndGet, status -> status >= 10, OPERATION,
                Duration.ofMinutes(5));

        // then — no wait exceeds MAX_DELAY_MILLIS and the cap is actually reached
        assertTrue(time.sleptMillis.stream().allMatch(delay -> delay <= MAX_DELAY_MILLIS));
        assertTrue(time.sleptMillis.contains(MAX_DELAY_MILLIS));
    }

    @Test
    void await_whenInterruptedDuringWait_throwsServerExceptionAndRestoresInterruptFlag() {
        // given — a sleeper that reports interruption
        CommandPoller.Sleeper interrupting = millis -> {
            throw new InterruptedException("test interrupt");
        };
        CommandPoller interruptedPoller = new CommandPoller(interrupting, () -> 0L,
                BASE_DELAY_MILLIS, MAX_DELAY_MILLIS, TIMEOUT);

        // then — surfaced as a server-trouble exception, interrupt flag preserved
        assertThrows(AllegroServerException.class,
                () -> interruptedPoller.await(() -> 0, status -> false, OPERATION));
        assertTrue(Thread.interrupted(), "interrupt flag must be restored");
    }

    @Test
    void await_whenDefaultTimeoutUsed_pollsWithoutExplicitDuration() {
        // given — the no-Duration overload uses the configured default
        VirtualTime time = new VirtualTime();
        AtomicInteger polls = new AtomicInteger();

        // when
        Integer result = poller(time).await(
                polls::incrementAndGet, status -> status >= 2, OPERATION);

        // then
        assertEquals(2, result);
        assertFalse(time.sleptMillis.isEmpty());
    }
}
