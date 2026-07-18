/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.StockProductItemRaw;
import org.jspecify.annotations.Nullable;

/**
 * One line of the fulfillment stock report: a product, its on-hand quantities,
 * recent selling stats, reserve health, and any storage fee. Read lazily via
 * {@code fulfillment().stock()}.
 *
 * @param product     the product this line describes
 * @param quantity    on-hand quantities
 * @param sellingStats recent selling velocity, when available
 * @param reserve     reserve-health assessment, when available
 * @param storageFee  storage-fee status, when available
 * @param offerId     the seller's offer bound to this product, when present
 *
 * @since 0.3.0
 */
public record StockItem(
        @Nullable StockProduct product,
        @Nullable StockQuantity quantity,
        @Nullable StockSellingStats sellingStats,
        @Nullable ReserveInfo reserve,
        @Nullable StockStorageFee storageFee,
        @Nullable String offerId) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static StockItem from(StockProductItemRaw raw) {
        return new StockItem(
                raw.getProduct() == null ? null : StockProduct.from(raw.getProduct()),
                raw.getQuantity() == null ? null : StockQuantity.from(raw.getQuantity()),
                raw.getSellingStats() == null ? null : StockSellingStats.from(raw.getSellingStats()),
                raw.getReserve() == null ? null : ReserveInfo.from(raw.getReserve()),
                raw.getStorageFee() == null ? null : StockStorageFee.from(raw.getStorageFee()),
                raw.getOfferId());
    }
}
