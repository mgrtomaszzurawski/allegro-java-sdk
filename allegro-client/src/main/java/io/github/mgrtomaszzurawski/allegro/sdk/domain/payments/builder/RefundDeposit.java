/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;

/**
 * A refundable deposit tied to one order line item, added to a
 * {@link RefundRequest} for a partial refund. A deposit (e.g. a bottle or
 * packaging deposit) is refunded by its total value.
 *
 * @since 0.7.0
 */
public final class RefundDeposit {

    private static final String ERR_LINE_ITEM_ID = "lineItemId is required";
    private static final String ERR_LINE_ITEM_ID_UUID = "lineItemId must be a UUID: ";
    private static final String ERR_TOTAL_VALUE = "totalValue is required";

    private final String lineItemId;
    private final Money totalValue;

    private RefundDeposit(String lineItemId, Money totalValue) {
        this.lineItemId = lineItemId;
        this.totalValue = totalValue;
    }

    /**
     * A deposit refund for the given line item.
     *
     * @param lineItemId the order line item's UUID id the deposit belongs to
     * @param totalValue the deposit's total value to refund
     * @return the deposit refund
     * @throws IllegalArgumentException if {@code lineItemId} is missing or not a
     *     UUID, or {@code totalValue} is null
     */
    public static RefundDeposit of(String lineItemId, Money totalValue) {
        return new RefundDeposit(
                RefundValidation.requireUuid(lineItemId, ERR_LINE_ITEM_ID, ERR_LINE_ITEM_ID_UUID),
                RefundValidation.requireNonNull(totalValue, ERR_TOTAL_VALUE));
    }

    /** The order line item's UUID id the deposit belongs to. */
    public String lineItemId() {
        return lineItemId;
    }

    /** The deposit's total value to refund. */
    public Money totalValue() {
        return totalValue;
    }
}
