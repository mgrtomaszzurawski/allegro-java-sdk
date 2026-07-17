/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * The client is misconfigured (invalid credentials shape, malformed URL,
 * contradictory options) — thrown fail-fast at construction time, never
 * mid-request. Remediation: fix the configuration.
 *
 * @since 0.1.0
 */
public class AllegroConfigException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AllegroConfigException(String message) {
        super(message, null);
    }

    public AllegroConfigException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
