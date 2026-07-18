/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPickupDropOffPointAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPickupDropOffPointRaw;
import org.jspecify.annotations.Nullable;

/**
 * An Allegro pickup / drop-off point a buyer can collect a parcel from (from
 * {@code orders().allegroPickupPoints(...)}).
 *
 * <p>A bounded core of the point: identity, kind and location. Richer detail
 * (opening hours, accepted payments, service restrictions) is available on the
 * wire and can be surfaced by later methods if a consumer needs it.
 *
 * @param id point identifier
 * @param name point name, or {@code null} when absent
 * @param type point kind (raw Allegro value), or {@code null} when absent
 * @param description free-text description, or {@code null} when absent
 * @param address the point's postal address, or {@code null} when absent
 *
 * @since 0.4.0
 */
public record PickupPoint(
        String id,
        @Nullable String name,
        @Nullable String type,
        @Nullable String description,
        @Nullable PointAddress address) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static PickupPoint from(AllegroPickupDropOffPointRaw raw) {
        var type = raw.getType();
        return new PickupPoint(
                raw.getId(),
                raw.getName(),
                type == null ? null : type.getValue(),
                raw.getDescription(),
                PointAddress.from(raw.getAddress()));
    }

    /**
     * A pickup point's postal address.
     *
     * @param street street and building, or {@code null}
     * @param postCode postal code, or {@code null}
     * @param city city, or {@code null}
     * @param countryCode ISO country code, or {@code null}
     */
    public record PointAddress(
            @Nullable String street,
            @Nullable String postCode,
            @Nullable String city,
            @Nullable String countryCode) {

        static @Nullable PointAddress from(@Nullable AllegroPickupDropOffPointAddressRaw raw) {
            if (raw == null) {
                return null;
            }
            return new PointAddress(
                    raw.getStreet(),
                    raw.getPostCode(),
                    raw.getCity(),
                    raw.getCountryCode());
        }
    }
}
