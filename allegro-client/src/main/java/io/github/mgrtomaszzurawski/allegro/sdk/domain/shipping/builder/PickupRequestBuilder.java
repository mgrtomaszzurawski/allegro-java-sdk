/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link PickupRequest}. The shipments, the pickup window
 * and the collection address are all required.
 *
 * @since 0.5.0
 */
public final class PickupRequestBuilder {

    private static final String FIELD_SHIPMENT_IDS = "PickupRequest.shipmentIds";
    private static final String FIELD_PICKUP_TIME = "PickupRequest.pickupTime";
    private static final String FIELD_ADDRESS = "PickupRequest.address";

    private @Nullable List<String> shipmentIds;
    private @Nullable PickupTime pickupTime;
    private @Nullable PostalAddress address;

    /** The shipments to collect (required; at least one). A defensive copy is taken. */
    public PickupRequestBuilder shipmentIds(@Nullable List<String> value) {
        this.shipmentIds = value == null ? null : List.copyOf(value);
        return this;
    }

    /** The requested pickup window (required). */
    public PickupRequestBuilder pickupTime(@Nullable PickupTime value) {
        this.pickupTime = value;
        return this;
    }

    /** The collection address (required). */
    public PickupRequestBuilder address(@Nullable PostalAddress value) {
        this.address = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link PickupRequest}.
     *
     * @throws IllegalStateException if a required field is missing
     */
    public PickupRequest build() {
        List<String> validShipmentIds =
                BuilderValidation.requireNonEmpty(shipmentIds, FIELD_SHIPMENT_IDS);
        PickupTime validPickupTime = BuilderValidation.requirePresent(pickupTime, FIELD_PICKUP_TIME);
        PostalAddress validAddress = BuilderValidation.requirePresent(address, FIELD_ADDRESS);
        return new PickupRequest(validShipmentIds, validPickupTime, validAddress);
    }
}
