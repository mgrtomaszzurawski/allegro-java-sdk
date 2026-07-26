/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupProposalsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link PickupProposalsRequest}. The shipments and the
 * collection address are required; the ready date is optional.
 *
 * @since 0.5.0
 */
public final class PickupProposalsRequestBuilder {

    private static final String FIELD_SHIPMENT_IDS = "PickupProposalsRequest.shipmentIds";
    private static final String FIELD_ADDRESS = "PickupProposalsRequest.address";

    private @Nullable List<String> shipmentIds;
    private @Nullable String readyDate;
    private @Nullable PostalAddress address;

    /** The shipments to collect (required; at least one). A defensive copy is taken. */
    public PickupProposalsRequestBuilder shipmentIds(@Nullable List<String> value) {
        this.shipmentIds = value == null ? null : List.copyOf(value);
        return this;
    }

    /** The date the shipments are ready (optional; ISO {@code yyyy-MM-dd}). */
    public PickupProposalsRequestBuilder readyDate(@Nullable String value) {
        this.readyDate = value;
        return this;
    }

    /** The collection address (required). */
    public PickupProposalsRequestBuilder address(@Nullable PostalAddress value) {
        this.address = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link PickupProposalsRequest}.
     *
     * @throws IllegalStateException if a required field is missing
     */
    public PickupProposalsRequest build() {
        List<String> validShipmentIds =
                BuilderValidation.requireNonEmpty(shipmentIds, FIELD_SHIPMENT_IDS);
        PostalAddress validAddress = BuilderValidation.requirePresent(address, FIELD_ADDRESS);
        return new PickupProposalsRequest(validShipmentIds, readyDate, validAddress);
    }
}
