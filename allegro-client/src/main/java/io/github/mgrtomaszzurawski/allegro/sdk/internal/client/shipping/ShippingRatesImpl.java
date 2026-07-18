/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.CreateShippingRatesSetUsingPOST201ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfShippingRatestUsingGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetShippingRatesSetUsingGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModifyShippingRatesSetUsingPUT200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.ShippingRates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link ShippingRates} facade — a stateless view
 * over the shared runtime.
 *
 * @since 0.3.0
 */
public final class ShippingRatesImpl implements ShippingRates {

    private static final String OP_LIST = "list shipping rates";
    private static final String OP_GET = "get shipping rate set";
    private static final String OP_CREATE = "create shipping rate set";
    private static final String OP_UPDATE = "update shipping rate set";

    private final HttpSupport http;

    public ShippingRatesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<ShippingRateSetSummary> list() {
        GetListOfShippingRatestUsingGET200ResponseRaw response = http.getAuthenticated(
                ApiPaths.SHIPPING_RATES, GetListOfShippingRatestUsingGET200ResponseRaw.class, OP_LIST);
        return mapSummaries(response.getShippingRates());
    }

    @Override
    public ShippingRateSet get(String rateSetId) {
        GetShippingRatesSetUsingGET200ResponseRaw raw = http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.SHIPPING_RATES, rateSetId),
                GetShippingRatesSetUsingGET200ResponseRaw.class, OP_GET);
        return ShippingRateSet.from(raw);
    }

    @Override
    public ShippingRateSet create(ShippingRateSetRequest request) {
        CreateShippingRatesSetUsingPOST201ResponseRaw created = http.postJsonAuthenticated(
                ApiPaths.SHIPPING_RATES, request.toCreateRaw(),
                CreateShippingRatesSetUsingPOST201ResponseRaw.class, OP_CREATE);
        return ShippingRateSet.from(created);
    }

    @Override
    public ShippingRateSet update(String rateSetId, ShippingRateSetRequest request) {
        ModifyShippingRatesSetUsingPUT200ResponseRaw updated = http.putJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.SHIPPING_RATES, rateSetId),
                request.toUpdateRaw(rateSetId),
                ModifyShippingRatesSetUsingPUT200ResponseRaw.class, OP_UPDATE);
        return ShippingRateSet.from(updated);
    }

    private static List<ShippingRateSetSummary> mapSummaries(
            @Nullable List<GetListOfShippingRatestUsingGET200ResponseShippingRatesInnerRaw> raw) {
        return raw == null ? List.of()
                : raw.stream().map(ShippingRateSetSummary::from).toList();
    }
}
