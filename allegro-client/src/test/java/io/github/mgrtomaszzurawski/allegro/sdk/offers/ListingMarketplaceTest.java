/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacePublicationStateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplacePublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplaceSellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ListingMarketplace;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MarketplacePublicationState;
import org.junit.jupiter.api.Test;

class ListingMarketplaceTest {

    private static final String PRICE = "899.00";
    private static final String CURRENCY = "CZK";

    @Test
    void from_whenAllBlocksAbsent_everyFieldIsNull() {
        // given — a per-marketplace entry carrying none of the optional blocks
        ListingMarketplace marketplace = ListingMarketplace.from(new OfferListingDtoV1AdditionalMarketplaceRaw());

        // then — the tolerant mapping degrades every field to null
        assertNull(marketplace.publicationState());
        assertNull(marketplace.price());
        assertNull(marketplace.priceAutomationRuleId());
        assertNull(marketplace.watchersCount());
        assertNull(marketplace.visitsCount());
        assertNull(marketplace.soldCount());
    }

    @Test
    void from_whenSellingModePresentWithoutPriceOrAutomation_pricesAreNull() {
        // given — a selling mode block with neither a price nor a price-automation rule
        OfferListingDtoV1AdditionalMarketplaceRaw raw = new OfferListingDtoV1AdditionalMarketplaceRaw()
                .sellingMode(new OfferListingDtoV1AdditionalMarketplaceSellingModeRaw());

        // then — the inner null-guards degrade both price fields to null
        ListingMarketplace marketplace = ListingMarketplace.from(raw);
        assertNull(marketplace.price());
        assertNull(marketplace.priceAutomationRuleId());
    }

    @Test
    void from_whenStatePresentAndPriceSet_mapsThem() {
        // given — a publication state and a per-marketplace price
        OfferListingDtoV1AdditionalMarketplaceRaw raw = new OfferListingDtoV1AdditionalMarketplaceRaw()
                .publication(new OfferListingDtoV1AdditionalMarketplacePublicationRaw()
                        .state(AdditionalMarketplacePublicationStateRaw.APPROVED))
                .sellingMode(new OfferListingDtoV1AdditionalMarketplaceSellingModeRaw()
                        .price(new BuyNowPriceRaw().amount(PRICE).currency(CURRENCY)));

        // then
        ListingMarketplace marketplace = ListingMarketplace.from(raw);
        assertEquals(MarketplacePublicationState.APPROVED, marketplace.publicationState());
        assertEquals(Money.of(PRICE, CURRENCY), marketplace.price());
    }
}
