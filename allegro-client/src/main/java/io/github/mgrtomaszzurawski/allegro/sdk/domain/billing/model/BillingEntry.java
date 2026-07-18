/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryBalanceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryTypeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One entry in the seller's billing ledger: a dated charge or credit, its
 * running balance, and the offer / order it relates to.
 *
 * <p>A bounded core of the billing entry; per-entry tax detail and free-form
 * additional info are available on the wire and can be surfaced by later methods.
 *
 * @param id billing entry identifier
 * @param occurredAt when the charge/credit was booked, or {@code null}
 * @param typeId billing type id (see {@code billing().types()}), or {@code null}
 * @param typeName billing type description, or {@code null}
 * @param value the signed amount of this entry, or {@code null} when absent
 * @param balance the account balance after this entry, or {@code null}
 * @param offerId the related offer id, or {@code null}
 * @param orderId the related order id, or {@code null}
 *
 * @since 0.5.0
 */
public record BillingEntry(
        String id,
        @Nullable OffsetDateTime occurredAt,
        @Nullable String typeId,
        @Nullable String typeName,
        @Nullable Money value,
        @Nullable Money balance,
        @Nullable String offerId,
        @Nullable String orderId) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static BillingEntry from(BillingEntryRaw raw) {
        BillingEntryTypeRaw type = raw.getType();
        BillingEntryValueRaw value = raw.getValue();
        BillingEntryBalanceRaw balance = raw.getBalance();
        BillingEntryOfferRaw offer = raw.getOffer();
        BillingEntryOrderRaw order = raw.getOrder();
        return new BillingEntry(
                raw.getId().toString(),
                raw.getOccurredAt(),
                type == null ? null : type.getId(),
                type == null ? null : type.getName(),
                value == null ? null : Money.of(value.getAmount(), value.getCurrency()),
                balance == null ? null : Money.of(balance.getAmount(), balance.getCurrency()),
                offer == null ? null : offer.getId(),
                order == null || order.getId() == null ? null : order.getId().toString());
    }
}
