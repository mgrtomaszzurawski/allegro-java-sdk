/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One purchased position on an order: a quantity of a single offer at the price
 * paid.
 *
 * @param id line-item identifier
 * @param offerId identifier of the purchased offer
 * @param offerName offer title at the time of purchase
 * @param quantity number of units bought
 * @param price unit price paid (already reflecting any discount)
 * @param boughtAt when this position was bought, or {@code null} when absent
 *
 * @since 0.3.0
 */
public record LineItem(
        String id,
        String offerId,
        String offerName,
        int quantity,
        Money price,
        @Nullable OffsetDateTime boughtAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static LineItem from(CheckoutFormLineItemRaw raw) {
        return new LineItem(
                raw.getId().toString(),
                raw.getOffer().getId(),
                raw.getOffer().getName(),
                raw.getQuantity().intValueExact(),
                Money.of(raw.getPrice().getAmount(), raw.getPrice().getCurrency()),
                raw.getBoughtAt());
    }
}
