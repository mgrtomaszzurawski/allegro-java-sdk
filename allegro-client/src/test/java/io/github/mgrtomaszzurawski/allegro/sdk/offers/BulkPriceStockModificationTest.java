/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.PriceChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.StockChange;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link BulkPriceStockModification} builder: fail-fast
 * validation and the discriminator/value each change kind serializes to. The
 * concrete change type is emitted by the request DTO's discriminator, so the
 * tests assert on the serialized JSON tree rather than the builder internals.
 */
class BulkPriceStockModificationTest {

    // NON_EMPTY mirrors the SDK's partial write body (betaJsonBodyPartial): unset
    // optional branches (null stock, empty price map) are omitted, not sent.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String OFFER_ID = "123456789";
    private static final String AMOUNT = "50.00";
    private static final String CURRENCY_PLN = "PLN";
    private static final String PERCENT = "-10.50%";
    private static final int STOCK_VALUE = 7;

    private static JsonNode toTree(BulkPriceStockModification modification) {
        return MAPPER.valueToTree(modification.toRaw());
    }

    @Test
    void forOffer_whenBlankOfferId_throws() {
        // given/when/then — a blank offer id is rejected at construction
        assertThrows(IllegalArgumentException.class,
                () -> BulkPriceStockModification.forOffer(" "));
    }

    @Test
    void build_whenNoPriceOrStockChange_throws() {
        // given — a builder with neither a price nor a stock change
        BulkPriceStockModification.Builder builder = BulkPriceStockModification.forOffer(OFFER_ID);

        // when/then — build fails fast
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void toRaw_whenFixedPriceAndFixedStock_emitsDiscriminatorsAndValues() {
        // given — a fixed marketplace price and a fixed stock
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.fixed(Money.of(AMOUNT, CURRENCY_PLN)))
                .stock(StockChange.fixed(STOCK_VALUE))
                .build();

        // when
        JsonNode tree = toTree(modification);

        // then — each change carries its discriminator and value
        JsonNode price = tree.at("/prices/" + MARKETPLACE_PL);
        assertEquals("FIXED", price.get("changeType").asText());
        assertEquals(AMOUNT, price.at("/value/amount").asText());
        assertEquals(CURRENCY_PLN, price.at("/value/currency").asText());
        assertEquals("FIXED", tree.at("/stock/changeType").asText());
        assertEquals(STOCK_VALUE, tree.at("/stock/value").asInt());
        assertEquals(OFFER_ID, tree.get("offerId").asText());
    }

    @Test
    void toRaw_whenPercentagePriceAndGainStock_emitsPercentageAndGain() {
        // given — a percentage price adjustment and a stock gain
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.percentage(PERCENT))
                .stock(StockChange.gain(STOCK_VALUE))
                .build();

        // when
        JsonNode tree = toTree(modification);

        // then — PERCENTAGE carries the percent string, GAIN carries the delta
        JsonNode price = tree.at("/prices/" + MARKETPLACE_PL);
        assertEquals("PERCENTAGE", price.get("changeType").asText());
        assertEquals(PERCENT, price.get("percentage").asText());
        assertTrue(price.at("/value").isMissingNode());
        assertEquals("GAIN", tree.at("/stock/changeType").asText());
        assertEquals(STOCK_VALUE, tree.at("/stock/value").asInt());
    }

    @Test
    void toRaw_whenPriceOnly_omitsStock() {
        // given — only a gain price change, no stock
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.gain(Money.of(AMOUNT, CURRENCY_PLN)))
                .build();

        // when
        JsonNode tree = toTree(modification);

        // then — the stock branch is absent, the price is a GAIN
        assertTrue(tree.at("/stock").isMissingNode());
        assertEquals("GAIN", tree.at("/prices/" + MARKETPLACE_PL + "/changeType").asText());
    }

    @Test
    void priceChange_whenNullAmount_throws() {
        // given/when/then — a null money amount is rejected
        assertThrows(NullPointerException.class, () -> PriceChange.fixed(null));
    }
}
