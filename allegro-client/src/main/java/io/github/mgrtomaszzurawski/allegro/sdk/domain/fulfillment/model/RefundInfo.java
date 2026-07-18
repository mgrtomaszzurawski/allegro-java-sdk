/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRefundDispositionRefundRaw;
import org.jspecify.annotations.Nullable;

/**
 * The refund tied to a {@link RefundDisposition}: its free-form status and
 * whether the seller needs to act ({@link RefundActionState}).
 *
 * @param status  server-side refund status text
 * @param details whether an action is needed from the seller
 *
 * @since 0.3.0
 */
public record RefundInfo(
        @Nullable String status,
        @Nullable RefundActionState details) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundInfo from(FulfillmentRefundDispositionRefundRaw raw) {
        return new RefundInfo(
                raw.getStatus(),
                raw.getDetails() == null ? null : RefundActionState.fromWire(raw.getDetails().getValue()));
    }
}
