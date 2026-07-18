/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentOrderParcelItemRaw;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One product line shipped inside a {@link Parcel}.
 *
 * @param productId      Allegro product identifier
 * @param offerId        the seller's offer bound to this product, when present
 * @param quantity       number of units in this parcel
 * @param serialNumbers  serial numbers of the units (never {@code null}; empty when none)
 * @param expirationDate expiry date of the units, when tracked
 *
 * @since 0.3.0
 */
public record ParcelItem(
        @Nullable String productId,
        @Nullable String offerId,
        @Nullable Integer quantity,
        List<String> serialNumbers,
        @Nullable LocalDate expirationDate) {

    public ParcelItem {
        serialNumbers = serialNumbers == null ? List.of() : List.copyOf(serialNumbers);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static ParcelItem from(FulfillmentOrderParcelItemRaw raw) {
        return new ParcelItem(
                raw.getProductId(),
                raw.getOfferId(),
                raw.getQuantity(),
                raw.getSerialNumbers(),
                raw.getExpirationDate());
    }
}
