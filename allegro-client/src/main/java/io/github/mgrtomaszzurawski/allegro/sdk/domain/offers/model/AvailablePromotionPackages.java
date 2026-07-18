/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackagesRaw;
import java.util.List;

/**
 * The promotion packages available to the seller — reached from
 * {@code offers().promoOptions().availablePackages()}. A <em>base</em> package
 * and any number of <em>extra</em> packages can be combined on an offer.
 *
 * @param basePackages  the selectable base packages
 * @param extraPackages the selectable add-on packages
 * @since 0.2.0
 */
public record AvailablePromotionPackages(
        List<PromotionPackage> basePackages,
        List<PromotionPackage> extraPackages) {

    public AvailablePromotionPackages {
        basePackages = List.copyOf(basePackages);
        extraPackages = List.copyOf(extraPackages);
    }

    /** Project the generated available-packages response onto the consumer record. */
    public static AvailablePromotionPackages from(AvailablePromotionPackagesRaw raw) {
        return new AvailablePromotionPackages(
                map(raw.getBasePackages()), map(raw.getExtraPackages()));
    }

    private static List<PromotionPackage> map(List<AvailablePromotionPackageRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(PromotionPackage::from).toList();
    }
}
