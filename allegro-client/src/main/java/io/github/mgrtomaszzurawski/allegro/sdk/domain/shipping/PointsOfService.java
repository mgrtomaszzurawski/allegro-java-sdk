/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Points of service — a seller's personal-collection locations (click &amp;
 * collect). Reached via {@code AllegroClient.shipping().points()}. Requires a
 * user-context token with {@code sale:settings:read} (for {@link #list} and
 * {@link #get}) and {@code sale:settings:write} (for {@link #create},
 * {@link #update} and {@link #delete}).
 *
 * @since 0.2.0
 */
public interface PointsOfService {

    /**
     * List the authenticated seller's points of service.
     *
     * <p>Allegro requires the seller id as a query parameter; the SDK resolves it
     * from the token, so the consumer does not pass their own id. The response is
     * not paginated — the seller's full set arrives in one call — so this is a
     * plain {@link List}, not a lazy stream.
     *
     * @return the seller's points of service, possibly empty
     */
    List<PointOfService> list();

    /**
     * List the authenticated seller's points of service in one country.
     *
     * @param countryCode ISO-3166 alpha-2 country filter (e.g. {@code "PL"}), or
     *     {@code null} to list points in every country
     * @return the seller's points of service, possibly empty
     */
    List<PointOfService> list(@Nullable String countryCode);

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
     * Modify an existing point of service.
     *
     * <p>The request is a full representation — Allegro replaces the point with
     * the supplied state (PUT semantics), so build it from every field the point
     * should keep, not only the ones that change.
     *
     * @param pointOfServiceId the point of service id
     * @param request the new point-of-service state
     * @return the updated point of service
     */
    PointOfService update(String pointOfServiceId, PointOfServiceRequest request);

    /**
     * Delete a point of service by id.
     *
     * @param pointOfServiceId the point of service id
     */
    void delete(String pointOfServiceId);
}
