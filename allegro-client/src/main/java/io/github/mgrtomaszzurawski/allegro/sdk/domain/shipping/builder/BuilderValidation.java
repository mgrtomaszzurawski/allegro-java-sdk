/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import org.jspecify.annotations.Nullable;

/**
 * Shared fail-fast checks for the shipping builders, so the required-field and
 * length rules read the same way in every builder.
 */
final class BuilderValidation {

    private static final String IS_REQUIRED = " is required";
    private static final String MUST_BE_AT_MOST = " must be at most ";
    private static final String CHARACTERS = " characters";

    private BuilderValidation() {
    }

    /** Require a non-blank string, else fail with {@code <field> is required}. */
    static String requireText(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + IS_REQUIRED);
        }
        return value;
    }

    /** Require a non-null value, else fail with {@code <field> is required}. */
    static <T> T requirePresent(@Nullable T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + IS_REQUIRED);
        }
        return value;
    }

    /** Reject a string longer than {@code maxLength} (a {@code null} value passes). */
    static void requireMaxLength(@Nullable String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalStateException(fieldName + MUST_BE_AT_MOST + maxLength + CHARACTERS);
        }
    }
}
