/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemDepositRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemDepositTotalValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * One deposit echoed back on a {@link PaymentRefund} — the refundable deposit
 * (e.g. packaging) tied to an order line item.
 *
 * @param lineItemId the order line item's id the deposit belongs to
 * @param totalValue the refundable deposit value, or {@code null} when absent
 *
 * @since 0.7.0
 */
public record RefundedDeposit(String lineItemId, @Nullable Money totalValue) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundedDeposit from(RefundLineItemDepositRaw raw) {
        RefundLineItemDepositTotalValueRaw totalValue = raw.getTotalValue();
        return new RefundedDeposit(
                raw.getLineItemId().toString(),
                totalValue == null ? null
                        : Money.of(totalValue.getAmount(), totalValue.getCurrency()));
    }
}
