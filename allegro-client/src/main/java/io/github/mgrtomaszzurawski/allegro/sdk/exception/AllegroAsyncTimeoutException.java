/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;

/**
 * Internal polling of an asynchronous Allegro command gave up before reaching
 * a terminal status. <strong>The operation may still have succeeded
 * server-side</strong> — remediation: check the resource state before
 * resubmitting.
 *
 * @since 0.1.0
 */
public class AllegroAsyncTimeoutException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AllegroAsyncTimeoutException(String message) {
        super(message, null);
    }
}
