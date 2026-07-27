/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PriceChangeRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.PriceChangeMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Wire-shape mapping of {@link PriceChangeMapper}: each price-change kind maps to
 * its discriminated {@code PriceModification} subtype (the {@code type} token the
 * subtype emits), the amount/currency holder, and the optional {@code marketplaceId}.
 * Assertions are on the serialized JSON tree; NON_EMPTY mirrors the SDK's partial body.
 */
class PriceChangeMapperTest {

    // Mirror the production mapper (AllegroClient) so the polymorphic PriceModification
    // discriminator (`type`) and the JsonNullable `marketplaceId` serialize as on the wire.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonNullableModule());
    private static final String OFFER_ONE = "111";
    private static final String OFFER_TWO = "222";
    private static final String AMOUNT = "149.50";
    private static final String CURRENCY_PLN = "PLN";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String PERCENTAGE = "10";
    private static final String TYPE_PATH = "/modification/type";
    private static final String FIXED_AMOUNT_PATH = "/modification/price/amount";
    private static final String FIXED_CURRENCY_PATH = "/modification/price/currency";
    private static final String CHANGE_AMOUNT_PATH = "/modification/value/amount";
    private static final String CHANGE_CURRENCY_PATH = "/modification/value/currency";
    private static final String PERCENTAGE_PATH = "/modification/percentage";
    private static final String MARKETPLACE_PATH = "/modification/marketplaceId";
    private static final String CRITERIA_TYPE_PATH = "/offerCriteria/0/type";
    private static final String CRITERIA_OFFER_ONE_ID_PATH = "/offerCriteria/0/offers/0/id";
    private static final String CRITERIA_OFFER_TWO_ID_PATH = "/offerCriteria/0/offers/1/id";
    private static final String TYPE_FIXED = "FIXED_PRICE";
    private static final String TYPE_INCREASE = "INCREASE_PRICE";
    private static final String TYPE_DECREASE = "DECREASE_PRICE";
    private static final String TYPE_INCREASE_PERCENTAGE = "INCREASE_PERCENTAGE";
    private static final String TYPE_DECREASE_PERCENTAGE = "DECREASE_PERCENTAGE";
    private static final String CRITERION_CONTAINS_OFFERS = "CONTAINS_OFFERS";

    private static JsonNode tree(PriceChangeRequest request) {
        return MAPPER.valueToTree(PriceChangeMapper.toRaw(request));
    }

    private static PriceChangeRequest.Builder forOne() {
        return PriceChangeRequest.forOffers(List.of(OFFER_ONE));
    }

    private static Money money() {
        return Money.of(AMOUNT, CURRENCY_PLN);
    }

    @Test
    void toRaw_whenFixedPrice_mapsFixedPriceSubtypeWithPriceHolder() {
        // given — a fixed price on two offers
        PriceChangeRequest request = PriceChangeRequest.forOffers(List.of(OFFER_ONE, OFFER_TWO))
                .setPrice(money())
                .build();

        // when
        JsonNode tree = tree(request);

        // then — FIXED_PRICE discriminator + price holder, no marketplace, both offers in the criterion
        assertEquals(TYPE_FIXED, tree.at(TYPE_PATH).asText());
        assertEquals(AMOUNT, tree.at(FIXED_AMOUNT_PATH).asText());
        assertEquals(CURRENCY_PLN, tree.at(FIXED_CURRENCY_PATH).asText());
        assertTrue(tree.at(MARKETPLACE_PATH).isMissingNode());
        assertEquals(CRITERION_CONTAINS_OFFERS, tree.at(CRITERIA_TYPE_PATH).asText());
        assertEquals(OFFER_ONE, tree.at(CRITERIA_OFFER_ONE_ID_PATH).asText());
        assertEquals(OFFER_TWO, tree.at(CRITERIA_OFFER_TWO_ID_PATH).asText());
    }

    @Test
    void toRaw_whenIncrease_mapsIncreaseSubtypeWithValueHolder() {
        // given — a relative price increase
        PriceChangeRequest request = forOne().increaseBy(money()).build();

        // when
        JsonNode tree = tree(request);

        // then — INCREASE_PRICE discriminator + value holder (not the fixed price holder)
        assertEquals(TYPE_INCREASE, tree.at(TYPE_PATH).asText());
        assertEquals(AMOUNT, tree.at(CHANGE_AMOUNT_PATH).asText());
        assertEquals(CURRENCY_PLN, tree.at(CHANGE_CURRENCY_PATH).asText());
        assertTrue(tree.at(FIXED_AMOUNT_PATH).isMissingNode());
    }

    @Test
    void toRaw_whenDecrease_mapsDecreaseSubtypeWithValueHolder() {
        // given — a relative price decrease
        PriceChangeRequest request = forOne().decreaseBy(money()).build();

        // when
        JsonNode tree = tree(request);

        // then — DECREASE_PRICE discriminator + value holder
        assertEquals(TYPE_DECREASE, tree.at(TYPE_PATH).asText());
        assertEquals(AMOUNT, tree.at(CHANGE_AMOUNT_PATH).asText());
        assertEquals(CURRENCY_PLN, tree.at(CHANGE_CURRENCY_PATH).asText());
    }

    @Test
    void toRaw_whenIncreaseByPercent_mapsPercentageIncreaseSubtype() {
        // given — a relative percentage increase
        PriceChangeRequest request = forOne().increaseByPercent(PERCENTAGE).build();

        // when
        JsonNode tree = tree(request);

        // then — INCREASE_PERCENTAGE discriminator + flat percentage (no value/price holder)
        assertEquals(TYPE_INCREASE_PERCENTAGE, tree.at(TYPE_PATH).asText());
        assertEquals(PERCENTAGE, tree.at(PERCENTAGE_PATH).asText());
        assertTrue(tree.at(CHANGE_AMOUNT_PATH).isMissingNode());
    }

    @Test
    void toRaw_whenDecreaseByPercent_mapsPercentageDecreaseSubtype() {
        // given — a relative percentage decrease
        PriceChangeRequest request = forOne().decreaseByPercent(PERCENTAGE).build();

        // when/then — DECREASE_PERCENTAGE discriminator + flat percentage, no value holder
        JsonNode tree = tree(request);
        assertEquals(TYPE_DECREASE_PERCENTAGE, tree.at(TYPE_PATH).asText());
        assertEquals(PERCENTAGE, tree.at(PERCENTAGE_PATH).asText());
        assertTrue(tree.at(CHANGE_AMOUNT_PATH).isMissingNode());
    }

    @Test
    void toRaw_whenMarketplaceSet_mapsMarketplaceId() {
        // given — a fixed price targeted at a specific marketplace
        PriceChangeRequest request = forOne().setPrice(money()).onMarketplace(MARKETPLACE_PL).build();

        // when/then — the marketplace id rides in the modification
        assertEquals(MARKETPLACE_PL, tree(request).at(MARKETPLACE_PATH).asText());
    }
}
