/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.Orders;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrappers behind the {@link Orders} facade.
 *
 * @since 0.3.0
 */
public final class OrdersImpl implements Orders {

    private static final String OP_GET_ORDER = "get order";

    private final HttpSupport http;

    public OrdersImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Order get(String orderId) {
        return Order.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId),
                CheckoutFormRaw.class, OP_GET_ORDER));
    }
}
