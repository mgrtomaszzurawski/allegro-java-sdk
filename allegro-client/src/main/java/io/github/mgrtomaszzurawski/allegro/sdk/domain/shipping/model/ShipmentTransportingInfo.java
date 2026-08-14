/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TransportingInfoDtoRaw;
import org.jspecify.annotations.Nullable;

/**
 * The carrier transporting information attached to a {@link ShipmentPackage}: the
 * carrier that moves the parcel and its carrier-side waybill number.
 *
 * @param carrierId the transporting carrier id, or {@code null} when not set
 * @param carrierWaybill the carrier-side waybill number, or {@code null} when not set
 *
 * @since 0.8.0
 */
public record ShipmentTransportingInfo(
        @Nullable String carrierId,
        @Nullable String carrierWaybill) {

    /** Map the generated Layer-1 DTO. */
    public static ShipmentTransportingInfo from(TransportingInfoDtoRaw raw) {
        return new ShipmentTransportingInfo(raw.getCarrierId(), raw.getCarrierWaybill());
    }
}
