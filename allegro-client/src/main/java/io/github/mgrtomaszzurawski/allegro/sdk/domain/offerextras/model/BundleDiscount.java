/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BundleDiscountDTORaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;

/**
 * The bundle discount applied on one marketplace — both when reading a bundle
 * ({@code OfferBundles.get(String)}) and when updating it
 * ({@code OfferBundles.updateDiscount(String, java.util.List)}).
 *
 * @param marketplaceId identifier of the marketplace the discount applies to
 * @param amount the discount amount
 *
 * @since 0.2.0
 */
public record BundleDiscount(String marketplaceId, Money amount) {

    /** Map one generated Layer-1 discount DTO to the public record. */
    static BundleDiscount from(BundleDiscountDTORaw raw) {
        return new BundleDiscount(
                raw.getMarketplace().getId(),
                Money.of(raw.getAmount(), raw.getCurrency()));
    }
}
