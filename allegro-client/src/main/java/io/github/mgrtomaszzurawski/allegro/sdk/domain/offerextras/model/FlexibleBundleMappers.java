/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleMarketplaceDiscountDTORaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Shared null-safe mappings for the flexible-bundle records: the per-marketplace
 * discount list and the two enums that this SDK maps by name with an
 * {@code UNKNOWN} forward-compat fallback.
 */
final class FlexibleBundleMappers {

    private FlexibleBundleMappers() {
    }

    /** Map a list of raw per-marketplace discounts, treating an absent list as empty. */
    static List<MarketplaceDiscount> marketplaceDiscounts(
            @Nullable List<FlexibleBundleMarketplaceDiscountDTORaw> raw) {
        return raw == null ? List.of() : raw.stream().map(MarketplaceDiscount::from).toList();
    }

    /** Map the wire {@code createdBy} name, degrading an unmodelled value to {@code UNKNOWN}. */
    static BundleCreatedBy createdBy(String rawName) {
        try {
            return BundleCreatedBy.valueOf(rawName);
        } catch (IllegalArgumentException unknownCreator) {
            return BundleCreatedBy.UNKNOWN;
        }
    }

    /** Map the wire discount-type name, degrading an unmodelled value to {@code UNKNOWN}. */
    static FlexibleBundleDiscountType discountType(String rawName) {
        try {
            return FlexibleBundleDiscountType.valueOf(rawName);
        } catch (IllegalArgumentException unknownType) {
            return FlexibleBundleDiscountType.UNKNOWN;
        }
    }
}
