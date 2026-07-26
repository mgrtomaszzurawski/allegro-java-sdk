/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRejectionRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * The seller's rejection of a customer return's refund, when one exists.
 *
 * <p>{@link #code()} is exposed as the raw Allegro string (e.g. {@code REFUND_REJECTED},
 * {@code ITEM_MISMATCH}) so the read surface stays forward-compatible with values Allegro
 * adds later; the write side uses the typed {@link ReturnRejectionCode} enum.
 *
 * @param code the rejection code (raw Allegro value), or {@code null}
 * @param reason the seller's reason, or {@code null}
 * @param createdAt when the rejection was created, or {@code null}
 *
 * @since 0.7.0
 */
public record ReturnRejection(
        @Nullable String code,
        @Nullable String reason,
        @Nullable OffsetDateTime createdAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnRejection from(CustomerReturnRejectionRaw raw) {
        var code = raw.getCode();
        return new ReturnRejection(
                code == null ? null : code.getValue(),
                raw.getReason(),
                raw.getCreatedAt());
    }
}
