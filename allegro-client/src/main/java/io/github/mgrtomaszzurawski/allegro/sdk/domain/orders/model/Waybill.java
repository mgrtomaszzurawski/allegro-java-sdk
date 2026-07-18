/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormAddWaybillCreatedLineItemsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormAddWaybillCreatedRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A parcel tracking number registered against an order: the carrier waybill and
 * the line items it covers.
 *
 * @param id the tracking-number record identifier assigned by Allegro
 * @param waybill the carrier's tracking (waybill) number
 * @param carrierId identifier of the carrier, or {@code null} when absent
 * @param carrierName human-readable carrier name, or {@code null} when absent
 * @param lineItemIds identifiers of the covered line items; never {@code null},
 *     possibly empty (empty means the whole order)
 * @param createdAt when the tracking number was registered, or {@code null}
 *
 * @since 0.4.0
 */
public record Waybill(
        String id,
        String waybill,
        @Nullable String carrierId,
        @Nullable String carrierName,
        List<String> lineItemIds,
        @Nullable OffsetDateTime createdAt) {

    public Waybill {
        lineItemIds = List.copyOf(lineItemIds);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static Waybill from(CheckoutFormAddWaybillCreatedRaw raw) {
        List<CheckoutFormAddWaybillCreatedLineItemsInnerRaw> lineItems = raw.getLineItems();
        List<String> lineItemIds = lineItems == null
                ? List.of()
                : lineItems.stream().map(CheckoutFormAddWaybillCreatedLineItemsInnerRaw::getId).toList();
        String createdAt = raw.getCreatedAt();
        return new Waybill(
                raw.getId(),
                raw.getWaybill(),
                raw.getCarrierId(),
                raw.getCarrierName(),
                lineItemIds,
                createdAt == null ? null : OffsetDateTime.parse(createdAt));
    }
}
