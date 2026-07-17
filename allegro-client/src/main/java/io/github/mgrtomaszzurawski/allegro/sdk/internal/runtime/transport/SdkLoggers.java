/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named SLF4J channels (ARCHITECTURE §11.1) — consumers enable exactly the
 * concern they debug: {@code …allegro.request}, {@code …allegro.auth},
 * {@code …allegro.retry}. DEBUG = lifecycle steps; WARN = self-healed
 * anomalies; the SDK never logs INFO/ERROR, bodies, or tokens.
 *
 * @since 0.1.0
 */
// Aggregating the three named channels is this holder's entire purpose —
// the one-logger-per-class rule targets accidental duplication, not a
// deliberate channel registry.
@SuppressWarnings("PMD.MoreThanOneLogger")
public final class SdkLoggers {

    private static final String CHANNEL_PREFIX = "io.github.mgrtomaszzurawski.allegro.";

    /** Request lifecycle: composed, serialized, sent, status+duration, mapped. */
    public static final Logger REQUEST = LoggerFactory.getLogger(CHANNEL_PREFIX + "request");
    /** Token lifecycle: cache, refresh, rotation, device flow, 401 replay. */
    public static final Logger AUTH = LoggerFactory.getLogger(CHANNEL_PREFIX + "auth");
    /** Retry decisions: attempt n/m, backoff, Retry-After. */
    public static final Logger RETRY = LoggerFactory.getLogger(CHANNEL_PREFIX + "retry");

    private SdkLoggers() {
    }
}
