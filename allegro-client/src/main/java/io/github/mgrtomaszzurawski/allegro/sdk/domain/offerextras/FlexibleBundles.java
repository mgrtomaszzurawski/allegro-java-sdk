/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleSummary;
import java.util.stream.Stream;

/**
 * The seller's flexible offer bundles — reached via
 * {@code AllegroClient.offers().flexibleBundles()}.
 *
 * <p>A flexible bundle is made of slots, each offering the buyer a choice of
 * offers, sold together at a whole-bundle or per-slot discount. This SDK version
 * covers reading the bundles ({@link #streamBundles()}, {@link #get(String)}) and
 * deleting them ({@link #delete(String)}); creating and updating flexible bundles
 * (the nested slot/offer/discount definition) is a planned follow-up. All
 * operations use the {@code sale:offers:*} scopes and need a user (seller) token.
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
     * Delete a flexible bundle.
     *
     * @param bundleId the bundle identifier
     */
    void delete(String bundleId);
}
