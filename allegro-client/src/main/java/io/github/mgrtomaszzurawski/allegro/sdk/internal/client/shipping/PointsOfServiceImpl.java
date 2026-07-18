/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.PosRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.PointsOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Endpoint wrapper behind the {@link PointsOfService} facade.
 *
 * @since 0.2.0
 */
public final class PointsOfServiceImpl implements PointsOfService {

    private static final String OP_CREATE = "create point of service";
    private static final String OP_GET = "get point of service";
    private static final String OP_DELETE = "delete point of service";

    private final HttpSupport http;

    public PointsOfServiceImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
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
    public void delete(String pointOfServiceId) {
        http.deleteAuthenticated(
                ApiPaths.subPath(ApiPaths.POINTS_OF_SERVICE, pointOfServiceId), OP_DELETE);
    }
}
