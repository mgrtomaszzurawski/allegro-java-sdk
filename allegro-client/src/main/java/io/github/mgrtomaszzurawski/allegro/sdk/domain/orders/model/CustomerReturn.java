/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnBuyerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRejectionRaw;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A customer return (BETA) — reached via {@code orders().returns()}: who returned
 * what, on which order, how the goods travel back, and — when the seller cannot
 * settle through Allegro — the buyer's bank account for a manual refund.
 *
 * @param id customer-return identifier, or {@code null} when absent
 * @param status the return's lifecycle status (raw Allegro value), or {@code null}
 * @param orderId the order the return is against, or {@code null}
 * @param referenceNumber the return's reference number, or {@code null}
 * @param buyerLogin the returning buyer's login, or {@code null}
 * @param buyerEmail the returning buyer's e-mail, or {@code null}
 * @param fulfillment {@code true} when the return is handled by Allegro fulfilment
 * @param itemCount number of returned items
 * @param items the returned items; empty when none
 * @param refundRejected {@code true} if the seller has rejected the refund
 * @param rejection the rejection detail, or {@code null} when not rejected
 * @param refundBankAccount the buyer's bank account for a manual refund, or {@code null}
 * @param parcels the return parcels; empty when none
 * @param marketplaceId the marketplace, or {@code null}
 * @param createdAt when the return was created, or {@code null}
 *
 * @since 0.6.0
 */
public record CustomerReturn(
        @Nullable String id,
        @Nullable String status,
        @Nullable String orderId,
        @Nullable String referenceNumber,
        @Nullable String buyerLogin,
        @Nullable String buyerEmail,
        boolean fulfillment,
        int itemCount,
        List<ReturnedItem> items,
        boolean refundRejected,
        @Nullable ReturnRejection rejection,
        @Nullable ReturnBankAccount refundBankAccount,
        List<ReturnParcel> parcels,
        @Nullable String marketplaceId,
        @Nullable OffsetDateTime createdAt) {

    /** Defensive, null-tolerant copies of the item and parcel collections. */
    public CustomerReturn {
        items = items == null ? List.of() : List.copyOf(items);
        parcels = parcels == null ? List.of() : List.copyOf(parcels);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static CustomerReturn from(CustomerReturnRaw raw) {
        CustomerReturnBuyerRaw buyer = raw.getBuyer();
        CustomerReturnRejectionRaw rejection = raw.getRejection();
        CustomerReturnRefundRaw refund = raw.getRefund();
        List<ReturnedItem> items = raw.getItems() == null ? List.of()
                : raw.getItems().stream().filter(Objects::nonNull)
                        .map(ReturnedItem::from).toList();
        List<ReturnParcel> parcels = raw.getParcels() == null ? List.of()
                : raw.getParcels().stream().filter(Objects::nonNull)
                        .map(ReturnParcel::from).toList();
        return new CustomerReturn(
                raw.getId(),
                raw.getStatus(),
                raw.getOrderId(),
                raw.getReferenceNumber(),
                buyer == null ? null : buyer.getLogin(),
                buyer == null ? null : buyer.getEmail(),
                Boolean.TRUE.equals(raw.getIsFulfillment()),
                items.size(),
                items,
                rejection != null,
                rejection == null ? null : ReturnRejection.from(rejection),
                refund == null || refund.getBankAccount() == null
                        ? null : ReturnBankAccount.from(refund.getBankAccount()),
                parcels,
                raw.getMarketplaceId(),
                raw.getCreatedAt());
    }

    /**
     * Redacts the buyer login and e-mail (personal data) so an accidental log or trace of a
     * {@code CustomerReturn} never leaks them; read {@link #buyerLogin()} / {@link #buyerEmail()}
     * deliberately. The bank account and parcels redact their own personal fields.
     */
    @Override
    public String toString() {
        return "CustomerReturn[id=" + id + ", status=" + status + ", orderId=" + orderId
                + ", itemCount=" + itemCount + ", refundRejected=" + refundRejected
                + ", buyer login/e-mail redacted]";
    }
}
