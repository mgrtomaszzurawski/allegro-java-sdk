/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleMarketplaceDiscountDTORaw;

/**
 * A flexible-bundle discount percentage on one marketplace.
 *
 * @param marketplaceId identifier of the marketplace
 * @param percentage the discount percentage
 *
 * @since 0.2.0
 */
public record MarketplaceDiscount(String marketplaceId, int percentage) {

    /** Map the generated Layer-1 DTO to the public record. */
    static MarketplaceDiscount from(FlexibleBundleMarketplaceDiscountDTORaw raw) {
        return new MarketplaceDiscount(raw.getMarketplaceId(), raw.getPercentage());
    }
}
