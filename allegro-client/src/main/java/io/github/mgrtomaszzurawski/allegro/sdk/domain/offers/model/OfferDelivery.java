/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * The delivery terms of an offer: which shipping-rate table applies, how long the
 * seller needs to hand a parcel over, and (for pre-orders) when it ships.
 *
 * <p>The same immutable value is used both ways: build one to set an offer's
 * delivery terms on {@code CreateOfferRequest}, or read one back from an
 * {@link Offer}. Every field is optional — a partial delivery block is valid, and
 * {@code shippingRatesId} references a shipping-rate table the seller has already
 * configured (its id is returned by the sale delivery-settings resource).
 *
 * @param shippingRatesId id of the seller's shipping-rate table, or {@code null}
 * @param handlingTime    handling time as an ISO-8601 duration (e.g. {@code "PT24H"}), or {@code null}
 * @param shipmentDate    declared shipment date for a pre-order offer, or {@code null}
 * @param additionalInfo  free-text delivery information, or {@code null}. Allegro is
 *                        retiring this field: on reads it is only populated on the
 *                        {@code allegro-pl} marketplace and is scheduled for removal —
 *                        prefer the shipping-rate table for delivery terms
 * @since 0.3.0
 */
public record OfferDelivery(
        @Nullable String shippingRatesId,
        @Nullable String handlingTime,
        @Nullable OffsetDateTime shipmentDate,
        @Nullable String additionalInfo) {

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-populated with this value's fields. */
    public Builder toBuilder() {
        return new Builder()
                .shippingRatesId(shippingRatesId)
                .handlingTime(handlingTime)
                .shipmentDate(shipmentDate)
                .additionalInfo(additionalInfo);
    }

    /**
     * Project a generated delivery response onto the consumer value.
     *
     * @param raw the generated delivery block (may be {@code null})
     * @return the mapped value, or {@code null} if {@code raw} is {@code null}
     */
    // getAdditionalInfo(): Allegro @Deprecated the response getter (field still
    // returned on allegro-pl), so mapping it for full coverage is intentional.
    @SuppressWarnings("deprecation")
    public static @Nullable OfferDelivery from(@Nullable DeliveryProductOfferResponseRaw raw) {
        if (raw == null) {
            return null;
        }
        JustIdRaw shippingRates = raw.getShippingRates();
        return builder()
                .shippingRatesId(shippingRates == null ? null : shippingRates.getId())
                .handlingTime(raw.getHandlingTime())
                .shipmentDate(raw.getShipmentDate())
                .additionalInfo(raw.getAdditionalInfo())
                .build();
    }

    /** Fluent builder for {@link OfferDelivery}. */
    public static final class Builder {

        private @Nullable String shippingRatesId;
        private @Nullable String handlingTime;
        private @Nullable OffsetDateTime shipmentDate;
        private @Nullable String additionalInfo;

        /** Reference the seller's configured shipping-rate table by id. */
        public Builder shippingRatesId(@Nullable String shippingRatesId) {
            this.shippingRatesId = shippingRatesId;
            return this;
        }

        /** Set the handling time as an ISO-8601 duration (e.g. {@code "PT24H"}). */
        public Builder handlingTime(@Nullable String handlingTime) {
            this.handlingTime = handlingTime;
            return this;
        }

        /** Set the declared shipment date (pre-order offers). */
        public Builder shipmentDate(@Nullable OffsetDateTime shipmentDate) {
            this.shipmentDate = shipmentDate;
            return this;
        }

        /** Set free-text delivery information shown to buyers. */
        public Builder additionalInfo(@Nullable String additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }

        /** Build the delivery terms. */
        public OfferDelivery build() {
            return new OfferDelivery(shippingRatesId, handlingTime, shipmentDate, additionalInfo);
        }
    }
}
