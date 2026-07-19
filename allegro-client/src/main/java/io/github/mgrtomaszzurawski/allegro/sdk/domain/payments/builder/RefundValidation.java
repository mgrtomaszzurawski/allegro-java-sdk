/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Shared fail-fast validation for the partial-refund request value types
 * ({@link RefundLineItem}, {@link RefundDeposit}, {@link RefundSurcharge}). A
 * consumer building a refund breakdown is rejected at construction time — an id
 * that is not a UUID or a missing amount never reaches the wire.
 */
final class RefundValidation {

    private RefundValidation() {
    }

    /** Require a non-blank UUID string, returning it unchanged. */
    static String requireUuid(@Nullable String value, String missingMessage, String notUuidMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(missingMessage);
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException notUuid) {
            throw new IllegalArgumentException(notUuidMessage + value, notUuid);
        }
        return value;
    }

    /** Require a non-null value, returning it unchanged. */
    static <T> T requireNonNull(@Nullable T value, String missingMessage) {
        if (value == null) {
            throw new IllegalArgumentException(missingMessage);
        }
        return value;
    }
}
