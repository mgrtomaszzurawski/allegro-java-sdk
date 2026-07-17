/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * A seller's offer as returned by {@code offers().get(offerId)}.
 *
 * <p>Immutable projection of the vendor payload onto the fields consumers work
 * with day to day. {@code buyNowPrice} is present only for a fixed-price sale
 * ({@link OfferFormat#BUY_NOW} / {@link OfferFormat#ADVERTISEMENT}); an auction
 * carries a starting price instead and leaves it {@code null}.
 * {@code availableStock} is absent for offers without a tracked quantity.
 *
 * @param id             offer identifier
 * @param name           offer title
 * @param categoryId     Allegro category the offer is listed in
 * @param format         how the offer is sold
 * @param status         publication status
 * @param buyNowPrice    fixed Buy Now price, or {@code null} for an auction
 * @param availableStock available quantity, or {@code null} when not tracked
 * @since 0.2.0
 */
public record Offer(
        String id,
        String name,
        String categoryId,
        OfferFormat format,
        OfferStatus status,
        @Nullable Money buyNowPrice,
        @Nullable Integer availableStock) {

    /** Project a generated product-offer response onto the consumer record. */
    public static Offer from(SaleProductOfferResponseV1Raw raw) {
        SellingModeRaw sellingMode = raw.getSellingMode();
        return new Offer(
                raw.getId(),
                raw.getName(),
                raw.getCategory().getId(),
                OfferFormat.from(sellingMode.getFormat()),
                OfferStatus.from(raw.getPublication().getStatus()),
                buyNowPriceOf(sellingMode),
                availableStockOf(raw));
    }

    private static @Nullable Money buyNowPriceOf(SellingModeRaw sellingMode) {
        BuyNowPriceRaw price = sellingMode.getPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Integer availableStockOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getStock() == null ? null : raw.getStock().getAvailable();
    }
}
