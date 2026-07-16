/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The request was rejected as invalid (HTTP 400/422) — remediation: fix the
 * request. Carries the typed field-level errors parsed from Allegro's
 * {@code errors[]} payload.
 *
 * @since 0.1.0
 */
public class AllegroBadRequestException extends AllegroException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<AllegroFieldError> errors;

    public AllegroBadRequestException(String message, int statusCode,
            @Nullable String responseBody, List<AllegroFieldError> errors) {
        super(message, statusCode, responseBody);
        this.errors = List.copyOf(errors);
    }

    /** Typed field-level errors; never {@code null}, possibly empty. */
    public List<AllegroFieldError> errors() {
        return errors;
    }
}
