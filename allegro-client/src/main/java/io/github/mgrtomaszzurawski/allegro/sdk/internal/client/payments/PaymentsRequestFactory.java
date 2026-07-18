/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.payments;

import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundPaymentRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import java.util.UUID;

/**
 * Builds the generated Layer-1 request body for the refund-initiation endpoint
 * from the public {@link RefundRequest}, keeping {@link PaymentsImpl} a thin
 * verb dispatcher.
 *
 * @since 0.5.0
 */
final class PaymentsRequestFactory {

    private PaymentsRequestFactory() {
    }

    /** Request body for {@code POST /payments/refunds}. */
    static InitializeRefundRaw initializeRefund(RefundRequest request) {
        return new InitializeRefundRaw()
                .payment(new RefundPaymentRaw().id(UUID.fromString(request.paymentId())))
                .order(new RefundOrderRaw().id(UUID.fromString(request.orderId())))
                .commandId(request.commandId())
                .reason(request.reason().toRaw());
    }
}
