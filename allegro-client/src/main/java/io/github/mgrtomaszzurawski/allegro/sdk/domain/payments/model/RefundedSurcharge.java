/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsSurchargeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundSurchargeValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * One surcharge echoed back on a {@link PaymentRefund} — a payment surcharge the
 * refund covers.
 *
 * @param surchargeId the surcharge's id
 * @param value the refunded surcharge value, or {@code null} when absent
 *
 * @since 0.7.0
 */
public record RefundedSurcharge(String surchargeId, @Nullable Money value) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundedSurcharge from(PaymentsSurchargeRaw raw) {
        RefundSurchargeValueRaw value = raw.getValue();
        return new RefundedSurcharge(
                raw.getId().toString(),
                value == null ? null : Money.of(value.getAmount(), value.getCurrency()));
    }
}
