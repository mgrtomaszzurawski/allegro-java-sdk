/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * One line item's share of a partial refund, added to a {@link RefundRequest}.
 * A line item is refunded in one of two ways, mutually exclusive and chosen by
 * the factory used:
 *
 * <ul>
 *   <li>{@link #byAmount(String, Money)} — refund a fixed {@link Money} amount of
 *       the line item (Allegro refund type {@code AMOUNT});</li>
 *   <li>{@link #byQuantity(String, BigDecimal)} — refund a number of units of the
 *       line item, priced by the server (Allegro refund type {@code QUANTITY}).</li>
 * </ul>
 *
 * <p>Exactly one of {@link #amount()} and {@link #quantity()} is non-null, so the
 * refund type is unambiguous.
 *
 * @since 0.7.0
 */
public final class RefundLineItem {

    private static final String ERR_LINE_ITEM_ID = "lineItemId is required";
    private static final String ERR_LINE_ITEM_ID_UUID = "lineItemId must be a UUID: ";
    private static final String ERR_AMOUNT = "amount is required";
    private static final String ERR_QUANTITY = "quantity is required";

    private final String lineItemId;
    private final @Nullable Money amount;
    private final @Nullable BigDecimal quantity;

    private RefundLineItem(String lineItemId, @Nullable Money amount, @Nullable BigDecimal quantity) {
        this.lineItemId = lineItemId;
        this.amount = amount;
        this.quantity = quantity;
    }

    /**
     * Refund a fixed amount of the given line item (refund type {@code AMOUNT}).
     *
     * @param lineItemId the order line item's UUID id
     * @param amount     the amount to refund for this line item
     * @return the line-item refund
     * @throws IllegalArgumentException if {@code lineItemId} is missing or not a
     *     UUID, or {@code amount} is null
     */
    public static RefundLineItem byAmount(String lineItemId, Money amount) {
        return new RefundLineItem(
                RefundValidation.requireUuid(lineItemId, ERR_LINE_ITEM_ID, ERR_LINE_ITEM_ID_UUID),
                RefundValidation.requireNonNull(amount, ERR_AMOUNT), null);
    }

    /**
     * Refund a number of units of the given line item (refund type
     * {@code QUANTITY}); the server prices the refund from the unit price.
     *
     * @param lineItemId the order line item's UUID id
     * @param quantity   the number of units to refund
     * @return the line-item refund
     * @throws IllegalArgumentException if {@code lineItemId} is missing or not a
     *     UUID, or {@code quantity} is null
     */
    public static RefundLineItem byQuantity(String lineItemId, BigDecimal quantity) {
        return new RefundLineItem(
                RefundValidation.requireUuid(lineItemId, ERR_LINE_ITEM_ID, ERR_LINE_ITEM_ID_UUID),
                null, RefundValidation.requireNonNull(quantity, ERR_QUANTITY));
    }

    /** The order line item's UUID id. */
    public String lineItemId() {
        return lineItemId;
    }

    /** The refunded amount for {@code AMOUNT} refunds, or {@code null} for {@code QUANTITY}. */
    public @Nullable Money amount() {
        return amount;
    }

    /** The refunded unit count for {@code QUANTITY} refunds, or {@code null} for {@code AMOUNT}. */
    public @Nullable BigDecimal quantity() {
        return quantity;
    }
}
