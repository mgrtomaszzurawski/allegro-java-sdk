/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A seller's view of one order (an Allegro <em>checkout form</em>): who bought,
 * what they bought, the amount due, and the current buyer-side and seller-side
 * statuses.
 *
 * <p>This record exposes the fields a seller needs to identify and process an
 * order, including the payment breakdown (main payment plus any surcharges) and
 * the seller's private note. The delivery and invoice breakdowns are added by a
 * later slice; see {@code docs/orders.md}.
 *
 * @param id order (checkout form) identifier
 * @param status buyer-side lifecycle status
 * @param sellerStatus seller handling status, or {@code null} when not set yet
 * @param buyer the buyer
 * @param lineItems purchased positions; never {@code null}, possibly empty
 * @param totalToPay total amount the buyer pays for this order
 * @param payment the main payment on the order, or {@code null} when unpaid
 * @param surcharges additional charges beyond the main payment; never
 *     {@code null}, possibly empty
 * @param messageToSeller buyer's message to the seller, or {@code null}
 * @param sellerNote the seller's private note on the order, or {@code null}
 * @param marketplaceId marketplace the order was placed on, or {@code null}
 * @param updatedAt last modification time, or {@code null} when absent
 * @param revision optimistic-concurrency token to pass back on status writes,
 *     or {@code null} when the order carries none
 *
 * @since 0.3.0
 */
public record Order(
        String id,
        OrderStatus status,
        @Nullable SellerStatus sellerStatus,
        Buyer buyer,
        List<LineItem> lineItems,
        Money totalToPay,
        @Nullable OrderPayment payment,
        List<OrderPayment> surcharges,
        @Nullable String messageToSeller,
        @Nullable String sellerNote,
        @Nullable String marketplaceId,
        @Nullable OffsetDateTime updatedAt,
        @Nullable String revision) {

    public Order {
        lineItems = List.copyOf(lineItems);
        surcharges = surcharges == null ? List.of() : List.copyOf(surcharges);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Order from(CheckoutFormRaw raw) {
        var totalToPay = raw.getSummary().getTotalToPay();
        var fulfillment = raw.getFulfillment();
        var marketplace = raw.getMarketplace();
        var payment = raw.getPayment();
        var note = raw.getNote();
        var surcharges = raw.getSurcharges();
        return new Order(
                raw.getId().toString(),
                OrderStatus.from(raw.getStatus()),
                SellerStatus.from(fulfillment == null ? null : fulfillment.getStatus()),
                Buyer.from(raw.getBuyer()),
                raw.getLineItems().stream().map(LineItem::from).toList(),
                Money.of(totalToPay.getAmount(), totalToPay.getCurrency()),
                payment == null ? null : OrderPayment.from(payment),
                surcharges == null ? List.of() : surcharges.stream().map(OrderPayment::from).toList(),
                raw.getMessageToSeller(),
                note == null ? null : note.getText(),
                marketplace == null ? null : marketplace.getId(),
                parseTimestamp(raw.getUpdatedAt()),
                raw.getRevision());
    }

    private static @Nullable OffsetDateTime parseTimestamp(@Nullable String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }
}
