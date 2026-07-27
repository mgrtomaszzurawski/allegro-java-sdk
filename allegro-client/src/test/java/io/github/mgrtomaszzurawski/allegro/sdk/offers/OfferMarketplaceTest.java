/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesRefusalReasonResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValuePublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MarketplacePublicationState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferMarketplace;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Projection of a per-marketplace response value onto the consumer read value. */
class OfferMarketplaceTest {

    private static final String AMOUNT = "899.00";
    private static final String CURRENCY = "CZK";
    private static final String MIN_AMOUNT = "500.00";
    private static final String START_AMOUNT = "600.00";
    private static final String REFUSAL_CODE = "PRICE_TOO_LOW";
    private static final String REFUSAL_MESSAGE = "The price is below the marketplace minimum.";
    private static final String PARAM_KEY = "min";
    private static final String PARAM_VALUE = "1000.00";

    @Test
    void from_whenBuyNowApproved_mapsFormatAndPriceAndState() {
        // given a per-marketplace value: BUY_NOW format, a price, and APPROVED publication
        AdditionalMarketplacesResponseValueRaw raw = new AdditionalMarketplacesResponseValueRaw()
                .sellingMode(new SellingModeRaw()
                        .format(SellingModeFormatRaw.BUY_NOW)
                        .price(new BuyNowPriceRaw().amount(AMOUNT).currency(CURRENCY)))
                .publication(new AdditionalMarketplacesResponseValuePublicationRaw()
                        .state(AdditionalMarketplacesResponseValuePublicationRaw.StateEnum.APPROVED));

        // when
        OfferMarketplace marketplace = OfferMarketplace.from(raw);

        // then
        assertEquals(OfferFormat.BUY_NOW, marketplace.format());
        assertEquals(AMOUNT, marketplace.price().amount());
        assertEquals(CURRENCY, marketplace.price().currency());
        assertEquals(MarketplacePublicationState.APPROVED, marketplace.publicationState());
        assertTrue(marketplace.refusalReasons().isEmpty());
    }

    @Test
    void from_whenAuctionPrices_mapsMinimalAndStartingPrice() {
        // given an auction selling mode with minimal + starting prices
        AdditionalMarketplacesResponseValueRaw raw = new AdditionalMarketplacesResponseValueRaw()
                .sellingMode(new SellingModeRaw()
                        .format(SellingModeFormatRaw.AUCTION)
                        .minimalPrice(new MinimalPriceRaw().amount(MIN_AMOUNT).currency(CURRENCY))
                        .startingPrice(new StartingPriceRaw().amount(START_AMOUNT).currency(CURRENCY)));

        // when
        OfferMarketplace marketplace = OfferMarketplace.from(raw);

        // then
        assertEquals(OfferFormat.AUCTION, marketplace.format());
        assertEquals(MIN_AMOUNT, marketplace.minimalPrice().amount());
        assertEquals(START_AMOUNT, marketplace.startingPrice().amount());
        assertNull(marketplace.price());
    }

    @Test
    void from_whenRefused_mapsStateAndRefusalReasons() {
        // given a REFUSED publication carrying a refusal reason with parameters
        AdditionalMarketplacesResponseValueRaw raw = new AdditionalMarketplacesResponseValueRaw()
                .publication(new AdditionalMarketplacesResponseValuePublicationRaw()
                        .state(AdditionalMarketplacesResponseValuePublicationRaw.StateEnum.REFUSED)
                        .refusalReasons(List.of(new AdditionalMarketplacesRefusalReasonResponseRaw()
                                .code(REFUSAL_CODE).userMessage(REFUSAL_MESSAGE)
                                .parameters(Map.of(PARAM_KEY, List.of(PARAM_VALUE))))));

        // when
        OfferMarketplace marketplace = OfferMarketplace.from(raw);

        // then
        assertEquals(MarketplacePublicationState.REFUSED, marketplace.publicationState());
        assertEquals(1, marketplace.refusalReasons().size());
        assertEquals(REFUSAL_CODE, marketplace.refusalReasons().get(0).code());
        assertEquals(REFUSAL_MESSAGE, marketplace.refusalReasons().get(0).userMessage());
        assertEquals(List.of(PARAM_VALUE), marketplace.refusalReasons().get(0).parameters().get(PARAM_KEY));
    }

    @Test
    void constructor_whenRefusalReasonsNull_normalizesToEmptyList() {
        // given a value constructed directly with a null refusalReasons list
        OfferMarketplace marketplace =
                new OfferMarketplace(null, null, null, null, null, null);

        // then the canonical constructor normalizes it to an immutable empty list
        assertTrue(marketplace.refusalReasons().isEmpty());
    }

    @Test
    void from_whenBlocksAbsent_degradesToNullsAndEmptyReasons() {
        // given an empty per-marketplace value (no sellingMode, no publication)
        OfferMarketplace marketplace = OfferMarketplace.from(new AdditionalMarketplacesResponseValueRaw());

        // then everything degrades to null / empty without throwing
        assertNull(marketplace.format());
        assertNull(marketplace.price());
        assertNull(marketplace.publicationState());
        assertTrue(marketplace.refusalReasons().isEmpty());
    }
}
