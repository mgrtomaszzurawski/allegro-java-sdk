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
 * {@code ITEM_MISMATCH}); the write side uses the typed {@link ReturnRejectionCode} enum. A
 * rejection code newer than this SDK's generated model surfaces as {@code null} (the Layer-1
 * enum collapses an unrecognized value to a sentinel, so the real string is not recoverable
 * here) — treat {@code null} as "present but unknown to this version".
 *
 * @param code the rejection code (raw Allegro value), or {@code null} when absent or unknown
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
        CustomerReturnRejectionRaw.CodeEnum code = raw.getCode();
        boolean unknown = code == null
                || code == CustomerReturnRejectionRaw.CodeEnum.UNKNOWN_DEFAULT_OPEN_API;
        return new ReturnRejection(
                unknown ? null : code.getValue(),
                raw.getReason(),
                raw.getCreatedAt());
    }
}
