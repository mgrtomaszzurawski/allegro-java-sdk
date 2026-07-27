/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceOfferPromoOptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsRaw;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The promotion packages currently applied to one offer — reached from
 * {@code offers().promoOptions().forOffer(offerId)}.
 *
 * @param offerId                the offer these options belong to
 * @param marketplaceId          the base marketplace these options apply to (e.g.
 *                               {@code allegro-pl}), or {@code null}
 * @param basePackage            the applied base package, or {@code null} if none
 * @param extraPackages          the applied add-on packages
 * @param pendingChanges         the package changes queued for the next cycle, or
 *                               {@code null} if none are pending
 * @param additionalMarketplaces the applied packages on each additional marketplace,
 *                               keyed by marketplace id (possibly empty)
 * @since 0.2.0
 */
public record OfferPromoOptions(
        @Nullable String offerId,
        @Nullable String marketplaceId,
        @Nullable AppliedPromoOption basePackage,
        List<AppliedPromoOption> extraPackages,
        @Nullable PendingPromoChanges pendingChanges,
        Map<String, MarketplacePromoOptions> additionalMarketplaces) {

    public OfferPromoOptions {
        extraPackages = List.copyOf(extraPackages);
        additionalMarketplaces = Map.copyOf(additionalMarketplaces);
    }

    /** Project the generated offer-promo-options response onto the consumer record. */
    public static OfferPromoOptions from(OfferPromoOptionsRaw raw) {
        OfferPromoOptionRaw base = raw.getBasePackage();
        List<OfferPromoOptionRaw> extras = raw.getExtraPackages();
        return new OfferPromoOptions(
                raw.getOfferId(),
                raw.getMarketplaceId(),
                base == null ? null : AppliedPromoOption.from(base),
                extras == null ? List.of() : extras.stream().map(AppliedPromoOption::from).toList(),
                PendingPromoChanges.from(raw.getPendingChanges()),
                additionalMarketplacesOf(raw.getAdditionalMarketplaces()));
    }

    private static Map<String, MarketplacePromoOptions> additionalMarketplacesOf(
            @Nullable List<MarketplaceOfferPromoOptionRaw> marketplaces) {
        if (marketplaces == null) {
            return Map.of();
        }
        Map<String, MarketplacePromoOptions> result = new HashMap<>();
        for (MarketplaceOfferPromoOptionRaw marketplace : marketplaces) {
            String id = marketplace.getMarketplaceId();
            if (id != null) {
                result.put(id, MarketplacePromoOptions.from(marketplace));
            }
        }
        return result;
    }
}
