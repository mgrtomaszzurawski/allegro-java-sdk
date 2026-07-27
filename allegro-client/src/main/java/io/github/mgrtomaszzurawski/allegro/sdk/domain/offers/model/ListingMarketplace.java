/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplacePublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplaceSellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1AdditionalMarketplaceStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1StatsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceAutomationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceAutomationRuleRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * How one {@link OfferSummary} is listed on a single additional (foreign) marketplace: its
 * publication state, per-marketplace Buy Now price and automatic-pricing rule, engagement stats,
 * and units sold there. A lighter per-marketplace view than the full offer's {@code OfferMarketplace}
 * — the listing payload carries only these fields, and marks none of them required, so every
 * component is {@code null} when the payload omits it.
 *
 * @param publicationState     whether — and how far along — the offer is published on the marketplace,
 *                             or {@code null}
 * @param price                the per-marketplace Buy Now price, or {@code null}
 * @param priceAutomationRuleId the id of the automatic-pricing rule applied on the marketplace, or
 *                             {@code null}
 * @param watchersCount        how many buyers watch the offer on the marketplace, or {@code null}
 * @param visitsCount          how many times the offer was visited on the marketplace, or {@code null}
 * @param soldCount            units sold on the marketplace, or {@code null}
 * @since 0.6.0
 */
public record ListingMarketplace(
        @Nullable MarketplacePublicationState publicationState,
        @Nullable Money price,
        @Nullable String priceAutomationRuleId,
        @Nullable Integer watchersCount,
        @Nullable Integer visitsCount,
        @Nullable Integer soldCount) {

    /** Project a generated per-marketplace listing entry onto the consumer record. */
    public static ListingMarketplace from(OfferListingDtoV1AdditionalMarketplaceRaw raw) {
        OfferListingDtoV1AdditionalMarketplacePublicationRaw publication = raw.getPublication();
        OfferListingDtoV1AdditionalMarketplaceSellingModeRaw sellingMode = raw.getSellingMode();
        OfferListingDtoV1StatsRaw stats = raw.getStats();
        OfferListingDtoV1AdditionalMarketplaceStockRaw stock = raw.getStock();
        return new ListingMarketplace(
                publication == null ? null : MarketplacePublicationState.from(publication.getState()),
                priceOf(sellingMode),
                priceAutomationRuleIdOf(sellingMode),
                stats == null ? null : stats.getWatchersCount(),
                stats == null ? null : stats.getVisitsCount(),
                stock == null ? null : stock.getSold());
    }

    private static @Nullable Money priceOf(@Nullable OfferListingDtoV1AdditionalMarketplaceSellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        BuyNowPriceRaw price = sellingMode.getPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable String priceAutomationRuleIdOf(
            @Nullable OfferListingDtoV1AdditionalMarketplaceSellingModeRaw sellingMode) {
        PriceAutomationRaw priceAutomation = sellingMode == null ? null : sellingMode.getPriceAutomation();
        PriceAutomationRuleRaw rule = priceAutomation == null ? null : priceAutomation.getRule();
        return rule == null ? null : rule.getId();
    }
}
