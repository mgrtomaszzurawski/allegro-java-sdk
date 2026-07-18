/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRefundDispositionRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One row of the refund-dispositions report: how returned or bounced goods were
 * handled when they came back into the warehouse, who is accountable, and the
 * refund tied to them. Read lazily via
 * {@code fulfillment().refundDispositions(filter)}.
 *
 * @param type                        why the goods came back (return or bounce)
 * @param refund                      the associated refund and its action state
 * @param stockStatus                 the state the goods were found in
 * @param verificationStatus          server-side verification status text
 * @param accountableForNonSellability who is accountable if the goods are non-sellable
 * @param orderId                     the order the goods came from
 * @param offerId                     the offer the goods were sold under
 * @param product                     the product involved
 * @param buyer                       the buyer involved, when present
 * @param createdAt                   when the disposition was recorded
 *
 * @since 0.3.0
 */
public record RefundDisposition(
        @Nullable RefundDispositionType type,
        @Nullable RefundInfo refund,
        @Nullable RefundStockStatus stockStatus,
        @Nullable String verificationStatus,
        @Nullable AccountableParty accountableForNonSellability,
        @Nullable String orderId,
        @Nullable String offerId,
        @Nullable RefundProduct product,
        @Nullable RefundBuyer buyer,
        @Nullable OffsetDateTime createdAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundDisposition from(FulfillmentRefundDispositionRaw raw) {
        return new RefundDisposition(
                raw.getType() == null ? null : RefundDispositionType.fromWire(raw.getType().getValue()),
                raw.getRefund() == null ? null : RefundInfo.from(raw.getRefund()),
                raw.getStockStatus() == null ? null
                        : RefundStockStatus.fromWire(raw.getStockStatus().getValue()),
                raw.getVerificationStatus(),
                raw.getAccountableForNonSellability() == null ? null
                        : AccountableParty.fromWire(raw.getAccountableForNonSellability().getValue()),
                raw.getOrderId(),
                raw.getOfferId(),
                raw.getProduct() == null ? null : RefundProduct.from(raw.getProduct()),
                raw.getBuyer() == null ? null : RefundBuyer.from(raw.getBuyer()),
                raw.getCreatedAt());
    }
}
