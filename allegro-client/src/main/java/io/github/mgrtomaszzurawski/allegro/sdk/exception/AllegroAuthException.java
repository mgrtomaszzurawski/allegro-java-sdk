/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Authentication or authorization failed (HTTP 401/403, a failed OAuth2 token
 * acquisition or refresh, or a scope the token does not carry) — remediation:
 * fix the credentials, re-authorize the user, or request the missing scope.
 *
 * @since 0.1.0
 */
public class AllegroAuthException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AllegroAuthException(String message, int statusCode, @Nullable String responseBody) {
        super(message, statusCode, responseBody);
    }

    public AllegroAuthException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
