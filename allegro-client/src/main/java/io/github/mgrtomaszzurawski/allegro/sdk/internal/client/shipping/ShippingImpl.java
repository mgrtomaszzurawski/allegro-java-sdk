/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.DeliverySettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.PointsOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.Shipping;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryMethod;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Entry point behind the {@link Shipping} facade. Root-level reads (delivery
 * methods) run through the shared {@link HttpSupport}; the sub-facades are
 * stateless views over the shared runtime, so each accessor returns a fresh
 * instance rather than caching (and exposing) a mutable field. The one piece of
 * retained state is the {@link SellerIdResolver}, held here so its cached
 * {@code GET /me} lookup is shared across those fresh sub-facades (it is never
 * exposed — {@link #points()} passes only its {@code sellerId} supplier).
 *
 * @since 0.2.0
 */
public final class ShippingImpl implements Shipping {

    private static final String OP_DELIVERY_METHODS = "list delivery methods";

    private final HttpRuntime runtime;
    private final HttpSupport http;
    private final SellerIdResolver sellerIdResolver;

    public ShippingImpl(HttpRuntime runtime) {
        this.runtime = runtime;
        this.http = new HttpSupport(runtime);
        // One resolver per client so the seller-id lookup is cached across the
        // fresh sub-facade instances that points() hands out.
        this.sellerIdResolver = new SellerIdResolver(runtime);
    }

    @Override
    public List<DeliveryMethod> deliveryMethods() {
        GetListOfDeliveryMethodsUsingGET200ResponseRaw response = http.getAuthenticated(
                ApiPaths.DELIVERY_METHODS, GetListOfDeliveryMethodsUsingGET200ResponseRaw.class,
                OP_DELIVERY_METHODS);
        return mapMethods(response.getDeliveryMethods());
    }

    @Override
    public PointsOfService points() {
        return new PointsOfServiceImpl(runtime, sellerIdResolver::sellerId);
    }

    @Override
    public DeliverySettings settings() {
        return new DeliverySettingsImpl(runtime);
    }

    private static List<DeliveryMethod> mapMethods(
            @Nullable List<GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(DeliveryMethod::from).toList();
    }
}
