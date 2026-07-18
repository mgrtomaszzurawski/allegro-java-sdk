/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferBundle;
import java.util.List;
import java.util.stream.Stream;

/**
 * The seller's fixed offer bundles — reached via
 * {@code AllegroClient.offers().bundles()}.
 *
 * <p>A fixed bundle groups a set of offers that a buyer can buy together at a
 * per-marketplace discount. All operations use the {@code sale:offers:*} scopes
 * and need a user (seller) token.
 *
 * @since 0.2.0
 */
public interface OfferBundles {

    /**
     * Stream the seller's fixed bundles, fetched page by page and lazily.
     *
     * @return a lazy stream of bundles
     */
    Stream<OfferBundle> streamBundles();

    /**
     * A single bundle by id.
     *
     * @param bundleId the bundle identifier
     * @return the bundle
     */
    OfferBundle get(String bundleId);

    /**
     * Replace a bundle's per-marketplace discounts.
     *
     * @param bundleId the bundle identifier
     * @param discounts the discounts to set, one per marketplace
     * @return the updated bundle
     */
    OfferBundle updateDiscount(String bundleId, List<BundleDiscount> discounts);

    /**
     * Delete a bundle.
     *
     * @param bundleId the bundle identifier
     */
    void delete(String bundleId);
}
