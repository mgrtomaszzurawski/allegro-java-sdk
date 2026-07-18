/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.PosRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SearchResultRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.PointsOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link PointsOfService} facade.
 *
 * @since 0.2.0
 */
public final class PointsOfServiceImpl implements PointsOfService {

    private static final String OP_LIST = "list points of service";
    private static final String OP_CREATE = "create point of service";
    private static final String OP_GET = "get point of service";
    private static final String OP_UPDATE = "update point of service";
    private static final String OP_DELETE = "delete point of service";

    private static final String PARAM_SELLER_ID = "seller.id";
    private static final String PARAM_COUNTRY_CODE = "countryCode";

    private final HttpSupport http;

    public PointsOfServiceImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<PointOfService> list(String sellerId) {
        return list(sellerId, null);
    }

    @Override
    public List<PointOfService> list(String sellerId, @Nullable String countryCode) {
        Query query = Query.create()
                .add(PARAM_SELLER_ID, sellerId)
                .add(PARAM_COUNTRY_CODE, countryCode);
        SearchResultRaw result = http.request(OP_LIST)
                .get(ApiPaths.POINTS_OF_SERVICE)
                .query(query)
                .fetch(SearchResultRaw.class);
        return mapPoints(result.getPosList());
    }

    @Override
    public PointOfService create(PointOfServiceRequest request) {
        PosRaw created = http.postJsonAuthenticated(
                ApiPaths.POINTS_OF_SERVICE, request.toRaw(), PosRaw.class, OP_CREATE);
        return PointOfService.from(created);
    }

    @Override
    public PointOfService get(String pointOfServiceId) {
        PosRaw raw = http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.POINTS_OF_SERVICE, pointOfServiceId), PosRaw.class, OP_GET);
        return PointOfService.from(raw);
    }

    @Override
    public PointOfService update(String pointOfServiceId, PointOfServiceRequest request) {
        PosRaw updated = http.putJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.POINTS_OF_SERVICE, pointOfServiceId),
                request.toRaw(), PosRaw.class, OP_UPDATE);
        return PointOfService.from(updated);
    }

    @Override
    public void delete(String pointOfServiceId) {
        http.deleteAuthenticated(
                ApiPaths.subPath(ApiPaths.POINTS_OF_SERVICE, pointOfServiceId), OP_DELETE);
    }

    private static List<PointOfService> mapPoints(@Nullable List<PosRaw> posList) {
        return posList == null ? List.of() : posList.stream().map(PointOfService::from).toList();
    }
}
