/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CoordinatesRaw;

/**
 * Geographic coordinates of a point of service.
 *
 * @param latitude WGS-84 latitude
 * @param longitude WGS-84 longitude
 *
 * @since 0.2.0
 */
public record Coordinates(double latitude, double longitude) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static Coordinates from(CoordinatesRaw raw) {
        return new Coordinates(raw.getLat(), raw.getLon());
    }

    /** Build the generated Layer-1 DTO for a request body. */
    public CoordinatesRaw toRaw() {
        CoordinatesRaw raw = new CoordinatesRaw();
        raw.setLat(latitude);
        raw.setLon(longitude);
        return raw;
    }
}
