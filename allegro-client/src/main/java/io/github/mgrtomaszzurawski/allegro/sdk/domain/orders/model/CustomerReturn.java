/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnBuyerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A customer return (BETA) — reached via {@code orders().returns()}. A bounded
 * core of the return: who returned what, on which order, and whether the seller
 * has already rejected the refund.
 *
 * @param id customer-return identifier
 * @param orderId the order the return is against, or {@code null}
 * @param referenceNumber the return's reference number, or {@code null}
 * @param buyerLogin the returning buyer's login, or {@code null}
 * @param itemCount number of returned items
 * @param refundRejected {@code true} if the seller has rejected the refund
 * @param marketplaceId the marketplace, or {@code null}
 * @param createdAt when the return was created, or {@code null}
 *
 * @since 0.6.0
 */
public record CustomerReturn(
        String id,
        @Nullable String orderId,
        @Nullable String referenceNumber,
        @Nullable String buyerLogin,
        int itemCount,
        boolean refundRejected,
        @Nullable String marketplaceId,
        @Nullable OffsetDateTime createdAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static CustomerReturn from(CustomerReturnRaw raw) {
        CustomerReturnBuyerRaw buyer = raw.getBuyer();
        List<CustomerReturnItemRaw> items = raw.getItems();
        return new CustomerReturn(
                raw.getId(),
                raw.getOrderId(),
                raw.getReferenceNumber(),
                buyer == null ? null : buyer.getLogin(),
                items == null ? 0 : items.size(),
                raw.getRejection() != null,
                raw.getMarketplaceId(),
                raw.getCreatedAt());
    }
}
