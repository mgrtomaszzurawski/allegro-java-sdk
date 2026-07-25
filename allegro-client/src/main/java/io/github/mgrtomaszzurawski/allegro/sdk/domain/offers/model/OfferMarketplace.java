/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValuePublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * How an offer appears on one additional (foreign) marketplace as read back from an {@link Offer}:
 * the per-marketplace selling terms (format and prices) and the publication outcome (state plus any
 * refusal reasons). Keyed by marketplace id in {@link Offer#additionalMarketplaces()}.
 *
 * @param format           the selling format on this marketplace — {@code null} when the
 *                         marketplace carries no selling mode, or {@link OfferFormat#UNKNOWN} when
 *                         the format is present but not one this release models
 * @param price            the Buy Now price on this marketplace, or {@code null}
 * @param minimalPrice     the minimal (auction) price on this marketplace, or {@code null}
 * @param startingPrice    the starting (auction) price on this marketplace, or {@code null}
 * @param publicationState the publication state on this marketplace, or {@code null}
 * @param refusalReasons   the reasons publication was refused (empty unless {@code REFUSED})
 * @since 0.6.0
 */
public record OfferMarketplace(
        @Nullable OfferFormat format,
        @Nullable Money price,
        @Nullable Money minimalPrice,
        @Nullable Money startingPrice,
        @Nullable MarketplacePublicationState publicationState,
        List<MarketplaceRefusalReason> refusalReasons) {

    /** Canonical constructor: normalizes {@code refusalReasons} to an immutable copy. */
    public OfferMarketplace {
        refusalReasons = refusalReasons == null ? List.of() : List.copyOf(refusalReasons);
    }

    /** Project a generated per-marketplace response value onto the consumer value. */
    public static OfferMarketplace from(AdditionalMarketplacesResponseValueRaw raw) {
        SellingModeRaw sellingMode = raw.getSellingMode();
        AdditionalMarketplacesResponseValuePublicationRaw publication = raw.getPublication();
        return new OfferMarketplace(
                sellingMode == null ? null : OfferFormat.from(sellingMode.getFormat()),
                priceOf(sellingMode == null ? null : sellingMode.getPrice()),
                minimalPriceOf(sellingMode == null ? null : sellingMode.getMinimalPrice()),
                startingPriceOf(sellingMode == null ? null : sellingMode.getStartingPrice()),
                publication == null ? null : MarketplacePublicationState.from(publication.getState()),
                refusalReasonsOf(publication));
    }

    private static @Nullable Money priceOf(@Nullable BuyNowPriceRaw price) {
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money minimalPriceOf(@Nullable MinimalPriceRaw price) {
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money startingPriceOf(@Nullable StartingPriceRaw price) {
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static List<MarketplaceRefusalReason> refusalReasonsOf(
            @Nullable AdditionalMarketplacesResponseValuePublicationRaw publication) {
        if (publication == null || publication.getRefusalReasons() == null) {
            return List.of();
        }
        return publication.getRefusalReasons().stream().map(MarketplaceRefusalReason::from).toList();
    }
}
