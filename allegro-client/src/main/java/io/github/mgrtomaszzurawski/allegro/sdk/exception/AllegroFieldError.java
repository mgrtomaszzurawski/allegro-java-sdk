/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serializable;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One entry of Allegro's structured {@code errors[]} payload — a typed,
 * per-field validation or business error.
 *
 * @param code machine-readable error identifier (e.g. {@code VALIDATION_ERROR})
 * @param message technical description addressed to the developer
 * @param userMessage message suitable for showing to an end user (often Polish)
 * @param path dot-notation location of the offending field
 *     (e.g. {@code category.id}), or {@code null} for request-level errors
 * @param details additional context, or {@code null}
 * @param metadata additional machine-readable key/value context for the error
 *     (possibly empty)
 *
 * @since 0.1.0
 */
public record AllegroFieldError(
        String code,
        String message,
        @Nullable String userMessage,
        @Nullable String path,
        @Nullable String details,
        Map<String, String> metadata) implements Serializable {

    /** Canonical constructor: defensively copies {@code metadata} to an immutable map. */
    public AllegroFieldError {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Convenience constructor for an error without {@code metadata}. */
    public AllegroFieldError(String code, String message, @Nullable String userMessage,
            @Nullable String path, @Nullable String details) {
        this(code, message, userMessage, path, details, Map.of());
    }
}
