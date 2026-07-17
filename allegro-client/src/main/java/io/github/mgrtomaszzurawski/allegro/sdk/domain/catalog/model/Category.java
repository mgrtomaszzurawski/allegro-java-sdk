/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDtoParentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDtoRaw;
import org.jspecify.annotations.Nullable;

/**
 * A node in the Allegro category tree.
 *
 * @param id opaque category identifier (integer- or UUID-shaped string)
 * @param name category display name, localized per the request's
 *     {@code Accept-Language} (marketplace default when unset)
 * @param leaf {@code true} when the category has no children; only leaf
 *     categories can hold offers
 * @param parentId identifier of the parent category, or {@code null} for a root
 * @param options what the category permits (product creation, advertisements,
 *     …), or {@code null} when Allegro omits it
 *
 * @since 0.2.0
 */
public record Category(
        String id,
        String name,
        boolean leaf,
        @Nullable String parentId,
        @Nullable CategoryOptions options) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Category from(CategoryDtoRaw raw) {
        CategoryDtoParentRaw parent = raw.getParent();
        return new Category(
                raw.getId(),
                raw.getName(),
                Boolean.TRUE.equals(raw.getLeaf()),
                parent == null ? null : parent.getId(),
                CategoryOptions.from(raw.getOptions()));
    }
}
