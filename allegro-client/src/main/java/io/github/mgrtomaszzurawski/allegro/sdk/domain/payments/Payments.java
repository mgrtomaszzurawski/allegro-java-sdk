/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.PaymentOperationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentRefund;
import java.util.stream.Stream;

/**
 * Payments for the authenticated seller — reached via {@code AllegroClient.payments()}:
 * the payment-operations history, refunded payments, and refund initiation.
 *
 * @since 0.5.0
 */
public interface Payments {

    /**
     * Lazily stream the seller's payment-operations history matching {@code filter},
     * pages fetched on demand.
     *
     * @param filter the operation filter ({@link PaymentOperationFilter#all()} for all)
     * @return a lazy stream of payment operations
     */
    Stream<PaymentOperation> streamOperations(PaymentOperationFilter filter);

    /**
     * Lazily stream the seller's refunded payments matching {@code filter}.
     *
     * @param filter the refund filter ({@link RefundFilter#all()} for all refunds)
     * @return a lazy stream of refunds
     */
    Stream<PaymentRefund> streamRefunds(RefundFilter filter);

    /**
     * Initiate a refund of a payment — the whole payment, or, when the request
     * carries line-item/deposit/surcharge/delivery/overpaid/additional-services
     * components, only those parts (a partial refund).
     *
     * @param request the refund request (payment id, order id, idempotency command
     *     id and reason, plus any partial-refund components)
     * @return the initiated refund, including the id and status Allegro assigned it
     */
    PaymentRefund refund(RefundRequest request);
}
