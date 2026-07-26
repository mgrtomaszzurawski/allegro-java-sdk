/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlreadyInWarehouseShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CourierBySellerShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CourierRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OwnTransportShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThirdPartyDeliveryShippingRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * How a shipment reaches the One Fulfillment warehouse — the Advance Ship Notice's
 * {@code shipping} declaration. A sealed set of the four delivery methods, each with
 * its own identifying details plus the common estimated arrival and departure country.
 *
 * <p>Three methods can be <em>declared</em> on a create/update ({@link CourierBySeller},
 * {@link OwnTransport}, {@link ThirdPartyDelivery}); {@link AlreadyInWarehouse} is
 * read-only — it is only ever read back, never sent, and the ASN write builders reject it.
 *
 * @since 0.7.0
 */
public sealed interface AsnShipping
        permits AsnShipping.CourierBySeller, AsnShipping.OwnTransport,
                AsnShipping.ThirdPartyDelivery, AsnShipping.AlreadyInWarehouse {

    /** The estimated date and time the shipment arrives at the warehouse. */
    OffsetDateTime estimatedTimeOfArrival();

    /** The ISO country code the shipment departs from. */
    String countryCode();

    /**
     * Delivered by a courier the seller booked.
     *
     * @param courierId            the courier service identifier (e.g. {@code "DPD"})
     * @param trackingNumbers      the parcel tracking numbers (never {@code null}; may be empty)
     * @param estimatedTimeOfArrival estimated arrival at the warehouse
     * @param countryCode          the ISO country the shipment departs from
     */
    record CourierBySeller(
            String courierId,
            List<String> trackingNumbers,
            OffsetDateTime estimatedTimeOfArrival,
            String countryCode) implements AsnShipping {

        public CourierBySeller {
            trackingNumbers = trackingNumbers == null ? List.of() : List.copyOf(trackingNumbers);
        }
    }

    /**
     * Delivered on the seller's own transport.
     *
     * @param truckLicencePlate    the licence plate of the delivering truck
     * @param estimatedTimeOfArrival estimated arrival at the warehouse
     * @param countryCode          the ISO country the shipment departs from
     */
    record OwnTransport(
            String truckLicencePlate,
            OffsetDateTime estimatedTimeOfArrival,
            String countryCode) implements AsnShipping {
    }

    /**
     * Delivered by a third-party carrier.
     *
     * @param carrierName          the carrier's name
     * @param orderNumber          the carrier's order number, or {@code null}
     * @param estimatedTimeOfArrival estimated arrival at the warehouse
     * @param countryCode          the ISO country the shipment departs from
     */
    record ThirdPartyDelivery(
            String carrierName,
            @Nullable String orderNumber,
            OffsetDateTime estimatedTimeOfArrival,
            String countryCode) implements AsnShipping {
    }

    /**
     * The goods are already in the warehouse (read-only — never declared on a write).
     *
     * @param estimatedTimeOfArrival estimated arrival at the warehouse
     * @param countryCode          the ISO country the shipment departs from
     */
    record AlreadyInWarehouse(
            OffsetDateTime estimatedTimeOfArrival,
            String countryCode) implements AsnShipping {
    }

    /**
     * Map the generated shipping DTO, or {@code null} when absent or of a method this
     * SDK release does not model (forward-compatible: an unknown method reads as {@code null}
     * rather than failing the whole notice).
     */
    static @Nullable AsnShipping from(@Nullable ShippingRaw raw) {
        if (raw instanceof CourierBySellerShippingRaw courier) {
            CourierRaw carrier = courier.getCourier();
            return new CourierBySeller(
                    carrier == null ? null : carrier.getId(),
                    carrier == null ? List.of() : carrier.getTrackingNumbers(),
                    courier.getEstimatedTimeOfArrival(),
                    courier.getCountryCode());
        }
        if (raw instanceof OwnTransportShippingRaw own) {
            return new OwnTransport(
                    own.getTruckLicencePlate(), own.getEstimatedTimeOfArrival(), own.getCountryCode());
        }
        if (raw instanceof ThirdPartyDeliveryShippingRaw third) {
            return new ThirdPartyDelivery(
                    third.getThirdParty() == null ? null : third.getThirdParty().getName(),
                    third.getThirdParty() == null ? null : third.getThirdParty().getOrderNumber(),
                    third.getEstimatedTimeOfArrival(),
                    third.getCountryCode());
        }
        if (raw instanceof AlreadyInWarehouseShippingRaw warehouse) {
            return new AlreadyInWarehouse(warehouse.getEstimatedTimeOfArrival(), warehouse.getCountryCode());
        }
        return null;
    }
}
