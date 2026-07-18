/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RejectionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ReturnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.CustomerReturn;
import java.util.stream.Stream;

/**
 * Customer returns (BETA) — reached via {@code orders().returns()}: browse the
 * buyer returns against the seller's orders and reject a return's refund.
 *
 * @since 0.6.0
 */
public interface CustomerReturns {

    /**
     * Lazily stream the seller's customer returns matching {@code filter}.
     *
     * @param filter the return filter ({@link ReturnFilter#all()} for every return)
     * @return a lazy stream of customer returns
     */
    Stream<CustomerReturn> streamReturns(ReturnFilter filter);

    /**
     * Fetch a single customer return by id.
     *
     * @param customerReturnId the customer-return identifier
     * @return the customer return
     */
    CustomerReturn get(String customerReturnId);

    /**
     * Reject the refund of a customer return.
     *
     * @param customerReturnId the customer-return identifier
     * @param request the rejection code and optional reason
     * @return the customer return after the rejection
     */
    CustomerReturn rejectRefund(String customerReturnId, RejectionRequest request);
}
