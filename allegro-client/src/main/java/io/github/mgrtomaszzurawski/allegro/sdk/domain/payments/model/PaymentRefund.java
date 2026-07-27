/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundAdditionalServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundOverpaidRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundDetailsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundPaymentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundTotalValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
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
 * <p>For a partial refund the server echoes back the breakdown it applied — the
 * {@link #lineItems()}, {@link #deposits()} and {@link #surcharges()} it refunded
 * and the {@link #delivery()}, {@link #overpaid()} and {@link #additionalServices()}
 * amounts. On a full refund those collections are empty and the amounts {@code null}.
 *
 * @param id refund identifier
 * @param status refund status, or {@code null}
 * @param reason refund reason, or {@code null}
 * @param totalValue total refunded amount, or {@code null} when absent
 * @param paymentId the refunded payment's id, or {@code null} when absent
 * @param orderId the related order's id, or {@code null}
 * @param createdAt when the refund was created, or {@code null}
 * @param lineItems the refunded line items; empty when none
 * @param deposits the refunded deposits; empty when none
 * @param delivery the refunded delivery amount, or {@code null} when none
 * @param overpaid the refunded overpaid amount, or {@code null} when none
 * @param additionalServices the refunded additional-services amount, or {@code null} when none
 * @param surcharges the refunded surcharges; empty when none
 * @param sellerComment the seller's justification for the refund, or {@code null}
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
        @Nullable OffsetDateTime createdAt,
        List<RefundedLineItem> lineItems,
        List<RefundedDeposit> deposits,
        @Nullable Money delivery,
        @Nullable Money overpaid,
        @Nullable Money additionalServices,
        List<RefundedSurcharge> surcharges,
        @Nullable String sellerComment) {

    /** Defensive, null-tolerant copies of the breakdown collections. */
    public PaymentRefund {
        lineItems = lineItems == null ? List.of() : List.copyOf(lineItems);
        deposits = deposits == null ? List.of() : List.copyOf(deposits);
        surcharges = surcharges == null ? List.of() : List.copyOf(surcharges);
    }

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
                raw.getCreatedAt(),
                mapLineItems(raw),
                mapDeposits(raw),
                deliveryAmount(raw.getDelivery()),
                overpaidAmount(raw.getOverpaid()),
                additionalServicesAmount(raw.getAdditionalServices()),
                mapSurcharges(raw),
                raw.getSellerComment());
    }

    private static List<RefundedLineItem> mapLineItems(RefundDetailsRaw raw) {
        return raw.getLineItems() == null ? List.of()
                : raw.getLineItems().stream().filter(Objects::nonNull)
                        .map(RefundedLineItem::from).toList();
    }

    private static List<RefundedDeposit> mapDeposits(RefundDetailsRaw raw) {
        return raw.getDeposits() == null ? List.of()
                : raw.getDeposits().stream().filter(Objects::nonNull)
                        .map(RefundedDeposit::from).toList();
    }

    private static List<RefundedSurcharge> mapSurcharges(RefundDetailsRaw raw) {
        return raw.getSurcharges() == null ? List.of()
                : raw.getSurcharges().stream().filter(Objects::nonNull)
                        .map(RefundedSurcharge::from).toList();
    }

    private static @Nullable Money deliveryAmount(@Nullable InitializeRefundDeliveryRaw raw) {
        if (raw == null || raw.getValue() == null) {
            return null;
        }
        return Money.of(raw.getValue().getAmount(), raw.getValue().getCurrency());
    }

    private static @Nullable Money overpaidAmount(@Nullable InitializeRefundOverpaidRaw raw) {
        if (raw == null || raw.getValue() == null) {
            return null;
        }
        return Money.of(raw.getValue().getAmount(), raw.getValue().getCurrency());
    }

    private static @Nullable Money additionalServicesAmount(
            @Nullable InitializeRefundAdditionalServicesRaw raw) {
        if (raw == null || raw.getValue() == null) {
            return null;
        }
        return Money.of(raw.getValue().getAmount(), raw.getValue().getCurrency());
    }
}
