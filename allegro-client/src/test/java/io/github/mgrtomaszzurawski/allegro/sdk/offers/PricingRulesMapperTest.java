/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingCommandRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange.CurrencyBasis;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.PricingRulesMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Wire-shape mapping of {@link PricingRulesMapper}: the {@code set}/{@code remove}
 * branches of the {@code oneOf} modification, the price-range configuration and
 * its currency basis, the {@code CONTAINS_OFFERS} criterion, and the omission of
 * an unset configuration. Assertions are on the serialized JSON tree; NON_EMPTY
 * mirrors the SDK's partial write body so unset optional fields are omitted.
 */
class PricingRulesMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new org.openapitools.jackson.nullable.JsonNullableModule())
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final UUID COMMAND_ID = UUID.fromString("123a08d7-ab9b-460d-b9cb-d6ed64b3a018");
    private static final String OFFER_ONE = "111";
    private static final String OFFER_TWO = "222";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String RULE_ID = "641c73feaef0a8281a3d11f8";
    private static final String MIN_PRICE = "10.00";
    private static final String MAX_PRICE = "500.00";
    private static final String CURRENCY_PLN = "PLN";

    private static JsonNode tree(BatchPricingRulesRequest request) {
        OfferAutomaticPricingCommandRaw raw = PricingRulesMapper.toRaw(COMMAND_ID, request);
        return MAPPER.valueToTree(raw);
    }

    @Test
    void toRaw_whenAssignWithRange_buildsSetBranchWithConfiguration() {
        // given — assign a rule bounded by a marketplace-currency price range
        BatchPricingRulesRequest request = BatchPricingRulesRequest.assignRules(List.of(OFFER_ONE))
                .onMarketplace(MARKETPLACE_PL, RULE_ID, PriceRange.of(
                        CurrencyBasis.MARKETPLACE_CURRENCY,
                        Money.of(MIN_PRICE, CURRENCY_PLN), Money.of(MAX_PRICE, CURRENCY_PLN)))
                .build();

        // when
        JsonNode tree = tree(request);

        // then — the command id, the SET branch with marketplace/rule/price-range,
        // and the offers as a CONTAINS_OFFERS criterion
        assertEquals(COMMAND_ID.toString(), tree.at("/id").asText());
        assertEquals(MARKETPLACE_PL, tree.at("/modification/set/0/marketplace/id").asText());
        assertEquals(RULE_ID, tree.at("/modification/set/0/rule/id").asText());
        assertEquals("MARKETPLACE_CURRENCY",
                tree.at("/modification/set/0/configuration/priceRange/type").asText());
        assertEquals(MIN_PRICE,
                tree.at("/modification/set/0/configuration/priceRange/minPrice/amount").asText());
        assertEquals(MAX_PRICE,
                tree.at("/modification/set/0/configuration/priceRange/maxPrice/amount").asText());
        assertEquals(CURRENCY_PLN,
                tree.at("/modification/set/0/configuration/priceRange/minPrice/currency").asText());
        assertEquals("CONTAINS_OFFERS", tree.at("/offerCriteria/0/type").asText());
        assertEquals(OFFER_ONE, tree.at("/offerCriteria/0/offers/0/id").asText());
        // and the remove branch is absent (oneOf)
        assertTrue(tree.at("/modification/remove").isMissingNode());
    }

    @Test
    void toRaw_whenAssignWithoutRange_omitsConfiguration() {
        // given — a config-less assignment
        BatchPricingRulesRequest request = BatchPricingRulesRequest.assignRules(List.of(OFFER_ONE))
                .onMarketplace(MARKETPLACE_PL, RULE_ID)
                .build();

        // when
        JsonNode tree = tree(request);

        // then — the optional configuration is omitted (partial body), not sent as null
        assertEquals(RULE_ID, tree.at("/modification/set/0/rule/id").asText());
        assertTrue(tree.at("/modification/set/0/configuration").isMissingNode());
    }

    @Test
    void toRaw_whenBaseMarketplaceCurrency_mapsType() {
        // given — a base-marketplace-currency price range (the other enum branch)
        BatchPricingRulesRequest request = BatchPricingRulesRequest.assignRules(List.of(OFFER_ONE))
                .onMarketplace(MARKETPLACE_PL, RULE_ID, PriceRange.of(
                        CurrencyBasis.BASE_MARKETPLACE_CURRENCY,
                        Money.of(MIN_PRICE, CURRENCY_PLN), Money.of(MAX_PRICE, CURRENCY_PLN)))
                .build();

        // when / then
        assertEquals("BASE_MARKETPLACE_CURRENCY",
                tree(request).at("/modification/set/0/configuration/priceRange/type").asText());
    }

    @Test
    void toRaw_whenAssignMultipleOffers_carriesEveryOffer() {
        // given — two offers targeted by the assignment
        BatchPricingRulesRequest request = BatchPricingRulesRequest.assignRules(List.of(OFFER_ONE, OFFER_TWO))
                .onMarketplace(MARKETPLACE_PL, RULE_ID)
                .build();

        // when
        JsonNode tree = tree(request);

        // then — both offers ride in the single CONTAINS_OFFERS criterion
        assertEquals(OFFER_ONE, tree.at("/offerCriteria/0/offers/0/id").asText());
        assertEquals(OFFER_TWO, tree.at("/offerCriteria/0/offers/1/id").asText());
    }

    @Test
    void toRaw_whenRemove_buildsRemoveBranchWithoutSet() {
        // given — remove the rules on one marketplace
        BatchPricingRulesRequest request = BatchPricingRulesRequest.removeRules(List.of(OFFER_ONE))
                .fromMarketplace(MARKETPLACE_PL)
                .build();

        // when
        JsonNode tree = tree(request);

        // then — the REMOVE branch carries the marketplace id, and no SET branch is sent
        assertEquals(MARKETPLACE_PL, tree.at("/modification/remove/0/marketplace/id").asText());
        assertTrue(tree.at("/modification/set").isMissingNode());
        assertEquals("CONTAINS_OFFERS", tree.at("/offerCriteria/0/type").asText());
    }
}
