/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RefundDetailsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundPaymentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundTotalValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A payment refund — the result read from {@code payments().streamRefunds(...)}
 * and returned by {@code payments().refund(...)}.
 *
 * <p>{@link #status()} and {@link #reason()} are exposed as the raw Allegro string
 * values (e.g. {@code SUCCESS}, {@code IN_PROGRESS}; {@code COMPLAINT}) rather than
 * SDK enums, so the public surface does not have to grow a constant every time
 * Allegro adds one. (Layer-1 currently deserializes these into strict generated
 * enums that reject an unknown value; relaxing that is the shared forward-compat
 * follow-up already filed for the generated models.)
 *
 * @param id refund identifier
 * @param status refund status, or {@code null}
 * @param reason refund reason, or {@code null}
 * @param totalValue total refunded amount, or {@code null} when absent
 * @param paymentId the refunded payment's id, or {@code null} when absent
 * @param orderId the related order's id, or {@code null}
 * @param createdAt when the refund was created, or {@code null}
 *
 * @since 0.5.0
 */
public record PaymentRefund(
        String id,
        @Nullable String status,
        @Nullable String reason,
        @Nullable Money totalValue,
        @Nullable String paymentId,
        @Nullable String orderId,
        @Nullable OffsetDateTime createdAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static PaymentRefund from(RefundDetailsRaw raw) {
        var status = raw.getStatus();
        var reason = raw.getReason();
        RefundTotalValueRaw totalValue = raw.getTotalValue();
        RefundPaymentRaw payment = raw.getPayment();
        RefundOrderRaw order = raw.getOrder();
        return new PaymentRefund(
                raw.getId().toString(),
                status == null ? null : status.getValue(),
                reason == null ? null : reason.getValue(),
                totalValue == null ? null : Money.of(totalValue.getAmount(), totalValue.getCurrency()),
                // Guard the id the same way as the order id below — a present
                // payment object with an absent id must not abort the stream.
                payment == null || payment.getId() == null ? null : payment.getId().toString(),
                order == null || order.getId() == null ? null : order.getId().toString(),
                raw.getCreatedAt());
    }
}
