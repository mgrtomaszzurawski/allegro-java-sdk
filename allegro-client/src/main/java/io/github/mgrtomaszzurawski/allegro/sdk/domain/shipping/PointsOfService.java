/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;

/**
 * Points of service — a seller's personal-collection locations (click &amp;
 * collect). Reached via {@code AllegroClient.shipping().points()}. Requires a
 * user-context token with the {@code sale:settings} scope.
 *
 * @since 0.2.0
 */
public interface PointsOfService {

    /**
     * Create a point of service.
     *
     * <p>Allegro returns {@code 409 Conflict} when a similar point of service
     * already exists (the existing point's URL is on the {@code Location}
     * response header); this SDK release surfaces that as the base
     * {@code AllegroException}. Give each point a distinct {@code name} /
     * {@code externalId} to avoid it.
     *
     * @param request the point of service to create
     * @return the created point of service, with its server-assigned id
     */
    PointOfService create(PointOfServiceRequest request);

    /**
     * Get a point of service by id.
     *
     * @param pointOfServiceId the point of service id
     * @return the point of service
     */
    PointOfService get(String pointOfServiceId);

    /**
     * Delete a point of service by id.
     *
     * @param pointOfServiceId the point of service id
     */
    void delete(String pointOfServiceId);
}
