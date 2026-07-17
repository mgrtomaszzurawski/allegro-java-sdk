/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;

/**
 * Orders, payments and billing for the authenticated seller — reached via
 * {@code AllegroClient.orders()}.
 *
 * <p>Starter slice of bucket B (orders-payments): only {@link #get(String)}
 * ships first, as the end-to-end proof of the orders facade against the shared
 * transport. Order listing and filtering, seller-status updates, parcel
 * tracking, invoices, customer returns, commission refunds, payments and
 * billing land in the bucket body per the task-division plan.
 *
 * @since 0.3.0
 */
public interface Orders {

    /**
     * Fetch a single order by its identifier.
     *
     * @param orderId the order (checkout form) identifier
     * @return the order details
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException
     *     if no order with that id exists for the authenticated seller
     */
    Order get(String orderId);
}
