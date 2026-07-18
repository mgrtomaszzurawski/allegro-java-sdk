/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleGetDTORaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A flexible bundle a seller has defined — a set of slots, each offering a choice
 * of offers, sold together at a whole-bundle or per-slot discount, as returned by
 * {@code FlexibleBundles.get(String)}.
 *
 * @param id the bundle identifier
 * @param createdBy who created the bundle
 * @param createdAt when the bundle was created
 * @param slots the bundle's slots; never {@code null}, possibly empty
 * @param discount the bundle's discount configuration, or {@code null} when none
 *     is set
 *
 * @since 0.2.0
 */
public record FlexibleBundle(
        String id,
        BundleCreatedBy createdBy,
        OffsetDateTime createdAt,
        List<FlexibleBundleSlot> slots,
        @Nullable FlexibleBundleDiscount discount) {

    public FlexibleBundle {
        slots = List.copyOf(slots);
    }

    /** Map the generated Layer-1 bundle DTO to the public record. */
    public static FlexibleBundle from(FlexibleBundleGetDTORaw raw) {
        return new FlexibleBundle(
                raw.getId().toString(),
                FlexibleBundleMappers.createdBy(raw.getCreatedBy().name()),
                raw.getCreatedAt(),
                raw.getSlots() == null
                        ? List.of()
                        : raw.getSlots().stream().map(FlexibleBundleSlot::from).toList(),
                raw.getDiscount() == null ? null : FlexibleBundleDiscount.from(raw.getDiscount()));
    }
}
