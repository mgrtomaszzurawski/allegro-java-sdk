/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackagesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceAvailablePromotionPackagesRaw;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The promotion packages available to the seller — reached from
 * {@code offers().promoOptions().availablePackages()}. A <em>base</em> package
 * and any number of <em>extra</em> packages can be combined on an offer.
 *
 * @param marketplaceId          the base marketplace these packages apply to (e.g.
 *                               {@code allegro-pl}), or {@code null}
 * @param basePackages           the selectable base packages
 * @param extraPackages          the selectable add-on packages
 * @param additionalMarketplaces the selectable packages on each additional marketplace,
 *                               keyed by marketplace id (possibly empty)
 * @since 0.2.0
 */
public record AvailablePromotionPackages(
        @Nullable String marketplaceId,
        List<PromotionPackage> basePackages,
        List<PromotionPackage> extraPackages,
        Map<String, MarketplaceAvailablePackages> additionalMarketplaces) {

    public AvailablePromotionPackages {
        basePackages = List.copyOf(basePackages);
        extraPackages = List.copyOf(extraPackages);
        additionalMarketplaces = Map.copyOf(additionalMarketplaces);
    }

    /** Project the generated available-packages response onto the consumer record. */
    public static AvailablePromotionPackages from(AvailablePromotionPackagesRaw raw) {
        return new AvailablePromotionPackages(
                raw.getMarketplaceId(),
                map(raw.getBasePackages()),
                map(raw.getExtraPackages()),
                additionalMarketplacesOf(raw.getAdditionalMarketplaces()));
    }

    private static List<PromotionPackage> map(@Nullable List<AvailablePromotionPackageRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(PromotionPackage::from).toList();
    }

    private static Map<String, MarketplaceAvailablePackages> additionalMarketplacesOf(
            @Nullable List<MarketplaceAvailablePromotionPackagesRaw> marketplaces) {
        if (marketplaces == null) {
            return Map.of();
        }
        Map<String, MarketplaceAvailablePackages> result = new HashMap<>();
        for (MarketplaceAvailablePromotionPackagesRaw marketplace : marketplaces) {
            String id = marketplace.getMarketplaceId();
            // keyed by marketplace id; an entry without one cannot be placed in the map
            if (id != null) {
                result.put(id, MarketplaceAvailablePackages.from(marketplace));
            }
        }
        return result;
    }
}
