/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * A seller's offer as returned by {@code offers().get(offerId)}.
 *
 * <p>Immutable projection of the vendor payload onto the fields consumers work
 * with day to day. A full product-offer read populates every field, but the
 * vendored spec marks none of the nested objects {@code required}, so mapping
 * tolerates their absence rather than risking an NPE on a partial payload:
 * {@code buyNowPrice} is {@code null} for an auction (which carries a starting
 * price) or when no selling mode is present; {@code availableStock} is
 * {@code null} for offers without a tracked quantity; {@code categoryId} is
 * {@code null} only if the payload omits the category.
 *
 * @param id             offer identifier
 * @param name           offer title
 * @param categoryId     Allegro category the offer is listed in, or {@code null}
 * @param format         how the offer is sold
 * @param status         publication status
 * @param buyNowPrice    fixed Buy Now price, or {@code null} for an auction
 * @param availableStock available quantity, or {@code null} when not tracked
 * @since 0.2.0
 */
public record Offer(
        String id,
        String name,
        @Nullable String categoryId,
        OfferFormat format,
        OfferStatus status,
        @Nullable Money buyNowPrice,
        @Nullable Integer availableStock) {

    /** Project a generated product-offer response onto the consumer record. */
    public static Offer from(SaleProductOfferResponseV1Raw raw) {
        SellingModeRaw sellingMode = raw.getSellingMode();
        OfferCategoryRaw category = raw.getCategory();
        SaleProductOfferPublicationResponseRaw publication = raw.getPublication();
        return new Offer(
                raw.getId(),
                raw.getName(),
                category == null ? null : category.getId(),
                OfferFormat.from(sellingMode == null ? null : sellingMode.getFormat()),
                OfferStatus.from(publication == null ? null : publication.getStatus()),
                buyNowPriceOf(sellingMode),
                availableStockOf(raw));
    }

    private static @Nullable Money buyNowPriceOf(@Nullable SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        BuyNowPriceRaw price = sellingMode.getPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Integer availableStockOf(SaleProductOfferResponseV1Raw raw) {
        return raw.getStock() == null ? null : raw.getStock().getAvailable();
    }
}
