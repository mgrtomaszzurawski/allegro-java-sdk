/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleListingDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundlesListingDTORaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A flexible bundle as it appears in the seller's bundle listing
 * ({@code FlexibleBundles.streamBundles()}) — the identity, discount, and one
 * representative offer id per slot. Fetch the full slots with
 * {@code FlexibleBundles.get(String)}.
 *
 * @param id the bundle identifier
 * @param createdBy who created the bundle
 * @param createdAt when the bundle was created
 * @param slotRepresentatives one representative offer id per slot; never
 *     {@code null}, possibly empty
 * @param discount the bundle's discount configuration, or {@code null} when none
 *     is set
 *
 * @since 0.2.0
 */
public record FlexibleBundleSummary(
        String id,
        BundleCreatedBy createdBy,
        OffsetDateTime createdAt,
        List<String> slotRepresentatives,
        @Nullable FlexibleBundleDiscount discount) {

    public FlexibleBundleSummary {
        slotRepresentatives = List.copyOf(slotRepresentatives);
    }

    /** Map one generated Layer-1 listing DTO to the public record. */
    static FlexibleBundleSummary from(FlexibleBundleListingDTORaw raw) {
        return new FlexibleBundleSummary(
                raw.getId(),
                FlexibleBundleMappers.createdBy(raw.getCreatedBy().name()),
                raw.getCreatedAt(),
                raw.getSlotsRepresentatives() == null ? List.of() : List.copyOf(raw.getSlotsRepresentatives()),
                raw.getDiscount() == null ? null : FlexibleBundleDiscount.from(raw.getDiscount()));
    }

    /** Map the generated Layer-1 listing response to public records. */
    public static List<FlexibleBundleSummary> listFrom(FlexibleBundlesListingDTORaw raw) {
        return raw.getBundles() == null
                ? List.of()
                : raw.getBundles().stream().map(FlexibleBundleSummary::from).toList();
    }
}
