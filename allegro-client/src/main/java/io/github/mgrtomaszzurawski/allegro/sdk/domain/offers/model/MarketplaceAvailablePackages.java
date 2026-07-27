/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceAvailablePromotionPackagesRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The promotion packages selectable on a single additional marketplace — a value
 * of {@link AvailablePromotionPackages#additionalMarketplaces()} (keyed by marketplace id).
 *
 * @param basePackages  the selectable base packages on this marketplace
 * @param extraPackages the selectable add-on packages on this marketplace
 * @since 0.7.0
 */
public record MarketplaceAvailablePackages(
        List<PromotionPackage> basePackages,
        List<PromotionPackage> extraPackages) {

    public MarketplaceAvailablePackages {
        basePackages = List.copyOf(basePackages);
        extraPackages = List.copyOf(extraPackages);
    }

    /** Project a generated per-marketplace available-packages block onto the consumer record. */
    public static MarketplaceAvailablePackages from(MarketplaceAvailablePromotionPackagesRaw raw) {
        return new MarketplaceAvailablePackages(map(raw.getBasePackages()), map(raw.getExtraPackages()));
    }

    private static List<PromotionPackage> map(@Nullable List<AvailablePromotionPackageRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(PromotionPackage::from).toList();
    }
}
