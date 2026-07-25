/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseAdditionalMarketplacesValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseSellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferPart;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Selected parts of an offer — the result of
 * {@code offers().getFields(offerId, OfferPart...)}. Only the requested
 * {@link OfferPart parts} are populated: {@code availableStock} for
 * {@link OfferPart#STOCK}, and {@code price} (base marketplace) plus
 * {@code marketplacePrices} (additional marketplaces) for {@link OfferPart#PRICE}.
 * A field the caller did not request — or that the offer does not carry — is
 * {@code null} / empty.
 *
 * @param id                the offer identifier
 * @param availableStock    available quantity, or {@code null} if STOCK was not requested
 * @param price             the base-marketplace Buy Now price, or {@code null} if PRICE was not requested
 * @param marketplacePrices the Buy Now price per additional marketplace (marketplace id → price)
 * @since 0.5.0
 */
public record PartialOffer(
        String id,
        @Nullable Integer availableStock,
        @Nullable Money price,
        Map<String, Money> marketplacePrices) {

    public PartialOffer {
        marketplacePrices = Map.copyOf(marketplacePrices);
    }

    /** Project a generated partial-offer response onto the consumer record. */
    public static PartialOffer from(SalePartialProductOfferResponseRaw raw) {
        return new PartialOffer(
                raw.getId(),
                availableStockOf(raw),
                priceOf(raw.getSellingMode()),
                marketplacePricesOf(raw));
    }

    private static @Nullable Integer availableStockOf(SalePartialProductOfferResponseRaw raw) {
        return raw.getStock() == null ? null : raw.getStock().getAvailable();
    }

    private static @Nullable Money priceOf(@Nullable SalePartialProductOfferResponseSellingModeRaw sellingMode) {
        return sellingMode == null ? null : moneyOf(sellingMode.getPrice());
    }

    private static Map<String, Money> marketplacePricesOf(SalePartialProductOfferResponseRaw raw) {
        Map<String, SalePartialProductOfferResponseAdditionalMarketplacesValueRaw> marketplaces =
                raw.getAdditionalMarketplaces();
        if (marketplaces == null) {
            return Map.of();
        }
        Map<String, Money> prices = new LinkedHashMap<>();
        marketplaces.forEach((marketplaceId, value) -> {
            Money marketplacePrice = value.getSellingMode() == null
                    ? null : moneyOf(value.getSellingMode().getPrice());
            if (marketplacePrice != null) {
                prices.put(marketplaceId, marketplacePrice);
            }
        });
        return prices;
    }

    private static @Nullable Money moneyOf(@Nullable PriceRaw price) {
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }
}
