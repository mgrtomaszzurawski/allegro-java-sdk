/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Server-side or transport trouble — HTTP 5xx, a network failure, or a request
 * timeout. One type on purpose: the consumer's remediation is identical
 * (retry later / alert), so distinguishing them adds catch blocks without
 * adding decisions.
 *
 * @since 0.1.0
 */
public class AllegroServerException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AllegroServerException(String message, int statusCode, @Nullable String responseBody) {
        super(message, statusCode, responseBody);
    }

    public AllegroServerException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
