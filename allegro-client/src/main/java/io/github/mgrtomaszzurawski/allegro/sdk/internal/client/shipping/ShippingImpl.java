/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.PointsOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.Shipping;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Entry point behind the {@link Shipping} facade. The sub-facades are stateless
 * views over the shared runtime, so each accessor returns a fresh instance
 * rather than caching (and exposing) a mutable field.
 *
 * @since 0.2.0
 */
public final class ShippingImpl implements Shipping {

    private final HttpRuntime runtime;

    public ShippingImpl(HttpRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public PointsOfService points() {
        return new PointsOfServiceImpl(runtime);
    }
}
