/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundRejectionRequestRejectionRaw;

/**
 * Reason a seller rejects a customer return's refund (the {@code code} sent to
 * {@code orders().returns().rejectRefund(...)}). Each constant name mirrors the
 * Allegro spec value exactly.
 *
 * @since 0.6.0
 */
public enum ReturnRejectionCode {

    /** The refund is rejected outright. */
    REFUND_REJECTED,

    /** A replacement item was sent instead of refunding. */
    NEW_ITEM_SENT,

    /** The item was repaired instead of refunding. */
    ITEM_FIXED,

    /** A missing part was sent instead of refunding. */
    MISSING_PART_SENT,

    /** The returned item does not match what was sold. */
    ITEM_MISMATCH,

    /** The purchase was a business purchase (no consumer return right). */
    BUSINESS_PURCHASE,

    /** The buyer has no return right for this purchase. */
    NO_RETURN_RIGHT;

    /** Map the public code to the generated Layer-1 enum for a rejection request. */
    public CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum toRaw() {
        return switch (this) {
            case REFUND_REJECTED -> CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.REFUND_REJECTED;
            case NEW_ITEM_SENT -> CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.NEW_ITEM_SENT;
            case ITEM_FIXED -> CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.ITEM_FIXED;
            case MISSING_PART_SENT ->
                    CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.MISSING_PART_SENT;
            case ITEM_MISMATCH -> CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.ITEM_MISMATCH;
            case BUSINESS_PURCHASE ->
                    CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.BUSINESS_PURCHASE;
            case NO_RETURN_RIGHT ->
                    CustomerReturnRefundRejectionRequestRejectionRaw.CodeEnum.NO_RETURN_RIGHT;
        };
    }
}
