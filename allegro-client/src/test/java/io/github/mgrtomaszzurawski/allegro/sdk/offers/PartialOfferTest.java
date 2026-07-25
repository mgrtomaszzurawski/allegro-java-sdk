/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseAdditionalMarketplacesValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseAdditionalMarketplacesValueSellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseSellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseStockRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PartialOffer;
import org.junit.jupiter.api.Test;

/**
 * Projection of the generated partial-offer response onto {@link PartialOffer}:
 * only the present parts populate the record, and absent parts stay
 * {@code null}/empty (the response only carries the parts the caller requested).
 */
class PartialOfferTest {

    private static final String OFFER_ID = "13579";
    private static final String MARKETPLACE_CZ = "allegro-cz";
    private static final String AMOUNT_PLN = "129.00";
    private static final String CURRENCY_PLN = "PLN";
    private static final String AMOUNT_CZK = "3200.00";
    private static final String CURRENCY_CZK = "CZK";
    private static final int STOCK = 42;

    private static PriceRaw price(String amount, String currency) {
        return new PriceRaw().amount(amount).currency(currency);
    }

    @Test
    void from_whenAllPartsPresent_mapsStockPriceAndMarketplacePrices() {
        // given — a response carrying stock, base price and one additional marketplace price
        SalePartialProductOfferResponseRaw raw = new SalePartialProductOfferResponseRaw()
                .id(OFFER_ID)
                .stock(new SalePartialProductOfferResponseStockRaw().available(STOCK))
                .sellingMode(new SalePartialProductOfferResponseSellingModeRaw()
                        .price(price(AMOUNT_PLN, CURRENCY_PLN)))
                .putAdditionalMarketplacesItem(MARKETPLACE_CZ,
                        new SalePartialProductOfferResponseAdditionalMarketplacesValueRaw()
                                .sellingMode(new SalePartialProductOfferResponseAdditionalMarketplacesValueSellingModeRaw()
                                        .price(price(AMOUNT_CZK, CURRENCY_CZK))));

        // when
        PartialOffer partial = PartialOffer.from(raw);

        // then
        assertEquals(OFFER_ID, partial.id());
        assertEquals(STOCK, partial.availableStock());
        assertEquals(AMOUNT_PLN, partial.price().amount());
        assertEquals(CURRENCY_PLN, partial.price().currency());
        assertEquals(AMOUNT_CZK, partial.marketplacePrices().get(MARKETPLACE_CZ).amount());
        assertEquals(CURRENCY_CZK, partial.marketplacePrices().get(MARKETPLACE_CZ).currency());
    }

    @Test
    void from_whenStockOnly_leavesPriceNullAndMarketplacePricesEmpty() {
        // given — a stock-only response (the caller requested only include=stock)
        SalePartialProductOfferResponseRaw raw = new SalePartialProductOfferResponseRaw()
                .id(OFFER_ID)
                .stock(new SalePartialProductOfferResponseStockRaw().available(STOCK));

        // when
        PartialOffer partial = PartialOffer.from(raw);

        // then
        assertEquals(STOCK, partial.availableStock());
        assertNull(partial.price());
        assertTrue(partial.marketplacePrices().isEmpty());
    }

    @Test
    void from_whenPriceOnly_leavesStockNull() {
        // given — a price-only response
        SalePartialProductOfferResponseRaw raw = new SalePartialProductOfferResponseRaw()
                .id(OFFER_ID)
                .sellingMode(new SalePartialProductOfferResponseSellingModeRaw()
                        .price(price(AMOUNT_PLN, CURRENCY_PLN)));

        // when
        PartialOffer partial = PartialOffer.from(raw);

        // then
        assertNull(partial.availableStock());
        assertEquals(AMOUNT_PLN, partial.price().amount());
    }

    @Test
    void from_whenMarketplaceHasNoPrice_skipsThatMarketplace() {
        // given — an additional marketplace whose selling mode carries no price
        SalePartialProductOfferResponseRaw raw = new SalePartialProductOfferResponseRaw()
                .id(OFFER_ID)
                .putAdditionalMarketplacesItem(MARKETPLACE_CZ,
                        new SalePartialProductOfferResponseAdditionalMarketplacesValueRaw());

        // when
        PartialOffer partial = PartialOffer.from(raw);

        // then — the price-less marketplace is not carried
        assertTrue(partial.marketplacePrices().isEmpty());
    }

    @Test
    void marketplacePrices_whenReadFromRecord_isImmutable() {
        // given — a mapped partial offer
        PartialOffer partial = PartialOffer.from(new SalePartialProductOfferResponseRaw().id(OFFER_ID)
                .stock(new SalePartialProductOfferResponseStockRaw().available(STOCK)));

        // then — the exposed marketplace-price map cannot be mutated by the caller
        assertThrows(UnsupportedOperationException.class,
                () -> partial.marketplacePrices().put(MARKETPLACE_CZ, null));
    }
}
