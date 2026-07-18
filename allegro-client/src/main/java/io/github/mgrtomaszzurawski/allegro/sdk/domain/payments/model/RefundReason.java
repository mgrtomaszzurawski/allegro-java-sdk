/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundRaw;

/**
 * Why a payment refund is being initiated. Each constant name mirrors the Allegro
 * spec value exactly.
 *
 * @since 0.5.0
 */
public enum RefundReason {

    /** A plain refund (e.g. the buyer changed their mind). */
    REFUND,

    /** A refund resulting from a complaint. */
    COMPLAINT,

    /** The product turned out to be unavailable. */
    PRODUCT_NOT_AVAILABLE,

    /** The buyer paid less than the amount due. */
    PAID_VALUE_TOO_LOW,

    /** The buyer overpaid; the surplus is returned. */
    OVERPAID,

    /** The order was cancelled by the buyer. */
    CANCELLED_BY_BUYER,

    /** The parcel was not collected. */
    NOT_COLLECTED;

    /** Map the public reason to the generated Layer-1 enum for a refund request. */
    public InitializeRefundRaw.ReasonEnum toRaw() {
        return switch (this) {
            case REFUND -> InitializeRefundRaw.ReasonEnum.REFUND;
            case COMPLAINT -> InitializeRefundRaw.ReasonEnum.COMPLAINT;
            case PRODUCT_NOT_AVAILABLE -> InitializeRefundRaw.ReasonEnum.PRODUCT_NOT_AVAILABLE;
            case PAID_VALUE_TOO_LOW -> InitializeRefundRaw.ReasonEnum.PAID_VALUE_TOO_LOW;
            case OVERPAID -> InitializeRefundRaw.ReasonEnum.OVERPAID;
            case CANCELLED_BY_BUYER -> InitializeRefundRaw.ReasonEnum.CANCELLED_BY_BUYER;
            case NOT_COLLECTED -> InitializeRefundRaw.ReasonEnum.NOT_COLLECTED;
        };
    }
}
