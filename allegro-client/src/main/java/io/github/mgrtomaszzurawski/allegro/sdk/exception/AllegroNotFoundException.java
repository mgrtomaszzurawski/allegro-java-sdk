/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * The referenced resource does not exist (HTTP 404 on a concrete resource) —
 * remediation: verify the identifier; this is a business condition, distinct
 * from server trouble.
 *
 * @since 0.1.0
 */
public class AllegroNotFoundException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AllegroNotFoundException(String message, int statusCode, @Nullable String responseBody) {
        super(message, statusCode, responseBody);
    }

    public AllegroNotFoundException(String message, int statusCode, @Nullable String responseBody,
            @Nullable String traceId) {
        super(message, null, statusCode, responseBody, traceId);
    }
}
