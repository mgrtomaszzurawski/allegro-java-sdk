/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListItemProductBasedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListProductBasedRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A compatibility list — the set of vehicles or parts an offer fits — as suggested
 * by Allegro for an offer or product.
 *
 * <p>A {@link CompatibilityListType#MANUAL MANUAL} list carries seller-supplied
 * {@link #items()} (each an id-picked or free-text {@link CompatibilityItem}) and
 * no {@link #id()}. A {@link CompatibilityListType#PRODUCT_BASED PRODUCT_BASED}
 * list is derived from the associated product: it carries that list's {@link #id()}
 * and a read-only text representation of its items, to be included in the offer
 * unchanged. A list type this SDK version does not model reads as
 * {@link CompatibilityListType#UNKNOWN} with empty items.
 *
 * @param type whether the list is manual or product-based
 * @param id the product-based list identifier for a {@code PRODUCT_BASED} list,
 *     otherwise {@code null}
 * @param items the compatible entries; empty when none are suggested
 *
 * @since 0.2.0
 */
public record CompatibilityList(
        CompatibilityListType type,
        @Nullable String id,
        List<CompatibilityItem> items) {

    /** Canonical constructor; defensively copies {@code items} immutable. */
    public CompatibilityList {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * A manual compatibility list over items already mapped from the response. The
     * manual and product-based generated subtypes do not share a common base, and
     * the manual list's items are a discriminated {@code oneOf} that the transport
     * resolves item-by-item before this factory (see {@code CompatibilityImpl}).
     *
     * @param items the mapped compatible entries
     * @return a manual list
     */
    public static CompatibilityList manual(List<CompatibilityItem> items) {
        return new CompatibilityList(CompatibilityListType.MANUAL, null, items);
    }

    /**
     * Map a generated Layer-1 product-based compatibility list to the public record.
     * Product-based items are plain text (not a {@code oneOf}), so the generated
     * subtype maps directly.
     *
     * @param raw the generated product-based list
     * @return the mapped product-based list
     */
    public static CompatibilityList fromProductBased(CompatibilityListProductBasedRaw raw) {
        return new CompatibilityList(
                CompatibilityListType.PRODUCT_BASED, raw.getId(), productBasedItems(raw.getItems()));
    }

    /**
     * The forward-compat landing for a list type Allegro introduced after this SDK
     * version: {@link CompatibilityListType#UNKNOWN} with empty items.
     *
     * @return an unknown-type list
     */
    public static CompatibilityList unknown() {
        return new CompatibilityList(CompatibilityListType.UNKNOWN, null, List.of());
    }

    private static List<CompatibilityItem> productBasedItems(
            @Nullable List<CompatibilityListItemProductBasedRaw> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(CompatibilityItem::fromProductBased).toList();
    }
}
