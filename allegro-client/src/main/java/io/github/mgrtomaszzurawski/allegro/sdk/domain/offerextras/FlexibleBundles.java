/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleSummary;
import java.util.stream.Stream;

/**
 * The seller's flexible offer bundles — reached via
 * {@code AllegroClient.offers().flexibleBundles()}.
 *
 * <p>A flexible bundle is made of slots, each offering the buyer a choice of
 * offers, sold together at a whole-bundle or per-slot discount. This facade covers
 * reading ({@link #streamBundles()}, {@link #get(String)}), creating
 * ({@link #create(FlexibleBundleRequest)}), updating
 * ({@link #update(String, FlexibleBundleRequest)}), and deleting
 * ({@link #delete(String)}) flexible bundles. All operations use the
 * {@code sale:offers:*} scopes and need a user (seller) token.
 *
 * @since 0.2.0
 */
public interface FlexibleBundles {

    /**
     * Stream the seller's flexible bundles (summaries), fetched page by page and
     * lazily.
     *
     * @return a lazy stream of bundle summaries
     */
    Stream<FlexibleBundleSummary> streamBundles();

    /**
     * A single flexible bundle by id, with its full slots and discount.
     *
     * @param bundleId the bundle identifier
     * @return the bundle
     */
    FlexibleBundle get(String bundleId);

    /**
     * Create a flexible bundle.
     *
     * @param request the bundle's slots and optional discount
     * @return the created bundle, with its assigned ids and per-marketplace
     *     availability
     */
    FlexibleBundle create(FlexibleBundleRequest request);

    /**
     * Update (replace) a flexible bundle.
     *
     * @param bundleId the bundle identifier
     * @param request the bundle's new slots and optional discount
     * @return the updated bundle
     */
    FlexibleBundle update(String bundleId, FlexibleBundleRequest request);

    /**
     * Delete a flexible bundle.
     *
     * @param bundleId the bundle identifier
     */
    void delete(String bundleId);
}
