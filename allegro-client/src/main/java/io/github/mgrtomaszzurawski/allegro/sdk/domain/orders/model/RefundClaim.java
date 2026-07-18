/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimBuyerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimCommissionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimLineItemOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimLineItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A commission-refund claim (from {@code orders().commissionRefunds()}): the
 * seller asks Allegro to refund the sales commission for a line item, e.g. after
 * a buyer cancellation.
 *
 * <p>{@link #status()} is exposed as the raw Allegro value for forward
 * compatibility.
 *
 * @param id claim identifier, or {@code null} when absent
 * @param status claim status (raw Allegro value), or {@code null}
 * @param quantity the claimed quantity, or {@code null}
 * @param commission the commission amount being reclaimed, or {@code null}
 * @param buyerId the buyer the line item was sold to, or {@code null}
 * @param lineItemId the line item the claim is for, or {@code null}
 * @param offerId the offer of that line item, or {@code null}
 * @param createdAt when the claim was created, or {@code null}
 *
 * @since 0.6.0
 */
public record RefundClaim(
        @Nullable String id,
        @Nullable String status,
        @Nullable Integer quantity,
        @Nullable Money commission,
        @Nullable String buyerId,
        @Nullable String lineItemId,
        @Nullable String offerId,
        @Nullable OffsetDateTime createdAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundClaim from(RefundClaimRaw raw) {
        var status = raw.getStatus();
        RefundClaimCommissionRaw commission = raw.getCommission();
        RefundClaimBuyerRaw buyer = raw.getBuyer();
        RefundClaimLineItemRaw lineItem = raw.getLineItem();
        RefundClaimLineItemOfferRaw offer = lineItem == null ? null : lineItem.getOffer();
        return new RefundClaim(
                raw.getId() == null ? null : raw.getId().toString(),
                status == null ? null : status.getValue(),
                raw.getQuantity(),
                commissionMoney(commission),
                buyer == null ? null : buyer.getId(),
                lineItem == null ? null : lineItem.getId(),
                offer == null ? null : offer.getId(),
                raw.getCreatedAt());
    }

    private static @Nullable Money commissionMoney(@Nullable RefundClaimCommissionRaw commission) {
        if (commission == null || commission.getAmount() == null || commission.getCurrency() == null) {
            return null;
        }
        return Money.of(commission.getAmount().toPlainString(), commission.getCurrency());
    }
}
