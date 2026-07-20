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
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link BulkPriceStockModification} builder: fail-fast
 * validation and the wire element(s) each change kind serializes to. Allegro
 * requires one change kind (price or stock) per {@code modifications[]} element,
 * so an offer that changes both is split into two elements; the concrete change
 * type is emitted by the request DTO's discriminator. The tests assert on the
 * serialized JSON tree rather than the builder internals.
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

    private static List<JsonNode> elements(BulkPriceStockModification modification) {
        return modification.toWireElements().stream()
                .map(element -> (JsonNode) MAPPER.valueToTree(element)).toList();
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
    void toWireElements_whenFixedPriceAndFixedStock_splitsIntoTwoSingleKindElements() {
        // given — a fixed marketplace price and a fixed stock on one offer
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.fixed(Money.of(AMOUNT, CURRENCY_PLN)))
                .stock(StockChange.fixed(STOCK_VALUE))
                .build();

        // when
        List<JsonNode> elements = elements(modification);

        // then — two elements, same offer, each carrying exactly one change kind
        assertEquals(2, elements.size());
        JsonNode priceElement = elements.get(0);
        assertEquals(OFFER_ID, priceElement.get("offerId").asText());
        assertEquals("FIXED", priceElement.at("/prices/" + MARKETPLACE_PL + "/changeType").asText());
        assertEquals(AMOUNT, priceElement.at("/prices/" + MARKETPLACE_PL + "/value/amount").asText());
        assertEquals(CURRENCY_PLN,
                priceElement.at("/prices/" + MARKETPLACE_PL + "/value/currency").asText());
        assertTrue(priceElement.at("/stock").isMissingNode());
        JsonNode stockElement = elements.get(1);
        assertEquals(OFFER_ID, stockElement.get("offerId").asText());
        assertEquals("FIXED", stockElement.at("/stock/changeType").asText());
        assertEquals(STOCK_VALUE, stockElement.at("/stock/value").asInt());
        assertTrue(stockElement.at("/prices").isMissingNode());
    }

    @Test
    void toWireElements_whenPercentagePriceAndGainStock_emitsPercentageAndGain() {
        // given — a percentage price adjustment and a stock gain
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.percentage(PERCENT))
                .stock(StockChange.gain(STOCK_VALUE))
                .build();

        // when
        List<JsonNode> elements = elements(modification);

        // then — PERCENTAGE carries the percent string (no value), GAIN carries the delta
        assertEquals(2, elements.size());
        JsonNode priceElement = elements.get(0);
        assertEquals("PERCENTAGE", priceElement.at("/prices/" + MARKETPLACE_PL + "/changeType").asText());
        assertEquals(PERCENT, priceElement.at("/prices/" + MARKETPLACE_PL + "/percentage").asText());
        assertTrue(priceElement.at("/prices/" + MARKETPLACE_PL + "/value").isMissingNode());
        assertEquals("GAIN", elements.get(1).at("/stock/changeType").asText());
        assertEquals(STOCK_VALUE, elements.get(1).at("/stock/value").asInt());
    }

    @Test
    void toWireElements_whenPriceOnly_singlePriceElement() {
        // given — only a gain price change, no stock
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .price(MARKETPLACE_PL, PriceChange.gain(Money.of(AMOUNT, CURRENCY_PLN)))
                .build();

        // when
        List<JsonNode> elements = elements(modification);

        // then — one element, a GAIN price, no stock branch
        assertEquals(1, elements.size());
        assertEquals("GAIN", elements.get(0).at("/prices/" + MARKETPLACE_PL + "/changeType").asText());
        assertTrue(elements.get(0).at("/stock").isMissingNode());
    }

    @Test
    void toWireElements_whenStockOnly_singleStockElement() {
        // given — only a fixed stock change, no price
        BulkPriceStockModification modification = BulkPriceStockModification.forOffer(OFFER_ID)
                .stock(StockChange.fixed(STOCK_VALUE))
                .build();

        // when
        List<JsonNode> elements = elements(modification);

        // then — one element, a FIXED stock, no prices branch
        assertEquals(1, elements.size());
        assertEquals("FIXED", elements.get(0).at("/stock/changeType").asText());
        assertTrue(elements.get(0).at("/prices").isMissingNode());
    }

    @Test
    void priceChange_whenNullAmount_throws() {
        // given/when/then — a null money amount is rejected
        assertThrows(NullPointerException.class, () -> PriceChange.fixed(null));
    }
}
