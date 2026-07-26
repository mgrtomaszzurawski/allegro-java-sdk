/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * One line item echoed back on a {@link PaymentRefund} — how much of a single
 * order line item the refund covers.
 *
 * <p>A refunded line item is expressed either by a fixed {@link #value() amount}
 * ({@code type} {@code AMOUNT}) or by a {@link #quantity() number of units}
 * ({@code type} {@code QUANTITY}); the other is then {@code null}. {@code type} is
 * exposed as the raw Allegro string (e.g. {@code AMOUNT}, {@code QUANTITY}) so the
 * public surface does not grow a constant each time Allegro adds one.
 *
 * @param lineItemId the order line item's id
 * @param type how the refunded portion is expressed, or {@code null}
 * @param value the refunded amount, or {@code null} when expressed by quantity
 * @param quantity the refunded number of units, or {@code null} when expressed by amount
 *
 * @since 0.7.0
 */
public record RefundedLineItem(
        String lineItemId,
        @Nullable String type,
        @Nullable Money value,
        @Nullable BigDecimal quantity) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundedLineItem from(RefundLineItemRaw raw) {
        var type = raw.getType();
        RefundLineItemValueRaw value = raw.getValue();
        return new RefundedLineItem(
                raw.getId().toString(),
                type == null ? null : type.getValue(),
                value == null ? null : Money.of(value.getAmount(), value.getCurrency()),
                raw.getQuantity());
    }
}
