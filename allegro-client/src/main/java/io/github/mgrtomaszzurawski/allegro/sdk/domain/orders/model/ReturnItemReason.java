/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnItemReasonRaw;
import org.jspecify.annotations.Nullable;

/**
 * Why a single returned item is being sent back.
 *
 * @param type the return reason (raw Allegro value), or {@code null}
 * @param userComment the buyer's free-text comment, or {@code null}
 *
 * @since 0.7.0
 */
public record ReturnItemReason(@Nullable String type, @Nullable String userComment) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnItemReason from(CustomerReturnItemReasonRaw raw) {
        return new ReturnItemReason(raw.getType(), raw.getUserComment());
    }
}
