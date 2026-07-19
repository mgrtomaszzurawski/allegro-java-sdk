/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;

/**
 * A refundable payment surcharge, added to a {@link RefundRequest} for a partial
 * refund. A surcharge (an extra charge beyond the line items, identified by its
 * own id) is refunded by its value.
 *
 * @since 0.7.0
 */
public final class RefundSurcharge {

    private static final String ERR_SURCHARGE_ID = "surchargeId is required";
    private static final String ERR_SURCHARGE_ID_UUID = "surchargeId must be a UUID: ";
    private static final String ERR_VALUE = "value is required";

    private final String surchargeId;
    private final Money value;

    private RefundSurcharge(String surchargeId, Money value) {
        this.surchargeId = surchargeId;
        this.value = value;
    }

    /**
     * A surcharge refund.
     *
     * @param surchargeId the surcharge's UUID id
     * @param value       the surcharge value to refund
     * @return the surcharge refund
     * @throws IllegalArgumentException if {@code surchargeId} is missing or not a
     *     UUID, or {@code value} is null
     */
    public static RefundSurcharge of(String surchargeId, Money value) {
        return new RefundSurcharge(
                RefundValidation.requireUuid(surchargeId, ERR_SURCHARGE_ID, ERR_SURCHARGE_ID_UUID),
                RefundValidation.requireNonNull(value, ERR_VALUE));
    }

    /** The surcharge's UUID id. */
    public String surchargeId() {
        return surchargeId;
    }

    /** The surcharge value to refund. */
    public Money value() {
        return value;
    }
}
