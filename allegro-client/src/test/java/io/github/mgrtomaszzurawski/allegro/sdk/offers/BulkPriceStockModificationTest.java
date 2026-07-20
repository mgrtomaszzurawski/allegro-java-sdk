/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.PriceChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.StockChange;
import org.junit.jupiter.api.Test;

/**
 * Fail-fast validation and accessors of the {@link BulkPriceStockModification}
 * builder. One failure test per required field (TESTING.md §1); the wire mapping
 * is covered separately by {@code BulkOfferModificationMapperTest}.
 */
class BulkPriceStockModificationTest {

    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String OFFER_ID = "123456789";
    private static final String AMOUNT = "50.00";
    private static final String CURRENCY_PLN = "PLN";
    private static final int STOCK_VALUE = 7;

    @Test
    void forOffer_whenBlankOfferId_throws() {
        // given/when/then — a blank offer id is rejected at construction, with a message
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> BulkPriceStockModification.forOffer(" "));
        assertEquals("offerId must not be null or blank", thrown.getMessage());
    }

    @Test
    void build_whenNoPriceOrStockChange_throws() {
        // given — a builder with neither a price nor a stock change
        BulkPriceStockModification.Builder builder = BulkPriceStockModification.forOffer(OFFER_ID);

        // when/then — build fails fast with a message
        IllegalStateException thrown = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("a modification must change at least a price or the stock", thrown.getMessage());
    }

    @Test
    void price_whenBlankMarketplace_throws() {
        // given/when/then — a blank marketplace id is rejected
        BulkPriceStockModification.Builder builder = BulkPriceStockModification.forOffer(OFFER_ID);
        assertThrows(IllegalArgumentException.class,
                () -> builder.price(" ", PriceChange.fixed(Money.of(AMOUNT, CURRENCY_PLN))));
    }

    @Test
    void price_whenNullChange_throws() {
        // given/when/then — a null price change is rejected
        BulkPriceStockModification.Builder builder = BulkPriceStockModification.forOffer(OFFER_ID);
        assertThrows(NullPointerException.class, () -> builder.price(MARKETPLACE_PL, null));
    }

    @Test
    void stock_whenNullChange_throws() {
        // given/when/then — a null stock change is rejected
        BulkPriceStockModification.Builder builder = BulkPriceStockModification.forOffer(OFFER_ID);
        assertThrows(NullPointerException.class, () -> builder.stock(null));
    }

    @Test
    void priceChangeFixed_whenNullAmount_throws() {
        // given/when/then — a null money amount is rejected
        assertThrows(NullPointerException.class, () -> PriceChange.fixed(null));
    }

    @Test
    void priceChangeGain_whenNullAmount_throws() {
        // given/when/then — a null money amount is rejected
        assertThrows(NullPointerException.class, () -> PriceChange.gain(null));
    }

    @Test
    void priceChangePercentage_whenNull_throws() {
        // given/when/then — a null percentage is rejected
        assertThrows(IllegalArgumentException.class, () -> PriceChange.percentage(null));
    }

    @Test
    void priceChangePercentage_whenBlank_throws() {
        // given/when/then — a blank percentage is rejected
        assertThrows(IllegalArgumentException.class, () -> PriceChange.percentage(" "));
    }

    @Test
    void stockChangeFixed_whenNotPositive_throws() {
        // given/when/then — a non-positive fixed stock is rejected (Allegro requires stock > 0)
        assertThrows(IllegalArgumentException.class, () -> StockChange.fixed(0));
        assertThrows(IllegalArgumentException.class, () -> StockChange.fixed(-1));
    }

    @Test
    void accessors_exposeBuiltData() {
        // given — a modification with a fixed price and a gain stock
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.fixed(Money.of(AMOUNT, CURRENCY_PLN)))
                .stock(StockChange.gain(STOCK_VALUE))
                .build();

        // then — the domain-typed accessors read the built intent back (no *Raw on the surface)
        assertEquals(OFFER_ID, modification.offerId());
        assertEquals(1, modification.prices().size());
        PriceChange price = modification.prices().get(MARKETPLACE_PL);
        assertEquals(PriceChange.Kind.FIXED, price.kind());
        assertEquals(AMOUNT, price.amount().amount());
        assertNull(price.percentage());
        assertEquals(StockChange.Kind.GAIN, modification.stock().kind());
        assertEquals(STOCK_VALUE, modification.stock().value());
    }

    @Test
    void prices_whenStockOnly_isEmptyAndStockNull() {
        // given — a stock-only modification
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .stock(StockChange.fixed(STOCK_VALUE))
                .build();

        // then — no price changes, stock present
        assertTrue(modification.prices().isEmpty());
        assertEquals(StockChange.Kind.FIXED, modification.stock().kind());
    }
}
