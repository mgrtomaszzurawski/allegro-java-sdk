/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormReferenceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventDataRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One entry in the seller's order event log: what changed, on which order, and
 * when. The event {@link #id()} is the cursor position — streaming resumes from
 * the last id seen.
 *
 * @param id event identifier (also the pagination cursor)
 * @param type the kind of change
 * @param occurredAt when the change happened
 * @param orderId the affected order's identifier, or {@code null} when the event
 *     carries no order reference
 * @param orderRevision the affected order's revision at event time, or
 *     {@code null} when absent
 *
 * @since 0.4.0
 */
public record OrderEvent(
        String id,
        OrderEventType type,
        OffsetDateTime occurredAt,
        @Nullable String orderId,
        @Nullable String orderRevision) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static OrderEvent from(OrderEventRaw raw) {
        OrderEventDataRaw order = raw.getOrder();
        CheckoutFormReferenceRaw checkoutForm = order == null ? null : order.getCheckoutForm();
        return new OrderEvent(
                raw.getId(),
                OrderEventType.from(raw.getType()),
                raw.getOccurredAt(),
                checkoutForm == null ? null : checkoutForm.getId(),
                checkoutForm == null ? null : checkoutForm.getRevision());
    }
}
