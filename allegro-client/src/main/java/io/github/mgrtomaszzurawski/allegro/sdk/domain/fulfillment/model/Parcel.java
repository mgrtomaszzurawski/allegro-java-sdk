/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentOrderParcelRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A single parcel shipped from the fulfillment warehouse for an order,
 * identified by its carrier waybill and carrying one or more {@link ParcelItem}s.
 *
 * @param waybill carrier waybill (tracking) number
 * @param items   product lines in this parcel (never {@code null}; empty when none)
 *
 * @since 0.3.0
 */
public record Parcel(
        @Nullable String waybill,
        List<ParcelItem> items) {

    public Parcel {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static Parcel from(FulfillmentOrderParcelRaw raw) {
        return new Parcel(
                raw.getWaybill(),
                raw.getItems() == null ? List.of()
                        : raw.getItems().stream().map(ParcelItem::from).toList());
    }
}
