/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import java.io.IOException;

/**
 * One runnable sandbox probe. Domain buckets implement a scenario (usually a
 * {@code <Feature>Demo} class in this package) and register it in
 * {@link DemoApp} with a single append-only line, so the write→read
 * verification tool grows without every bucket editing the same dispatch.
 *
 * @since 0.2.0
 */
@FunctionalInterface
public interface DemoScenario {

    /**
     * Run the probe against the sandbox.
     *
     * @param clientId     sandbox application client id (from the environment)
     * @param clientSecret sandbox application client secret (from the environment)
     * @param account      {@code seller} or {@code buyer} — selects the stored token
     * @throws IOException on a token-store or transport failure
     */
    void run(String clientId, String clientSecret, String account) throws IOException;
}
