/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryEventBaseCategoryParentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryEventBaseCategoryRaw;
import org.jspecify.annotations.Nullable;

/**
 * The category a {@link CategoryEvent} is about, as it stood at the time of the
 * event.
 *
 * @param id the category identifier
 * @param name the category name
 * @param leaf whether the category is a leaf (holds offers), or {@code null} when
 *     the event payload omits it
 * @param parentId the parent category identifier, or {@code null} for a root
 *
 * @since 0.2.0
 */
public record CategoryEventCategory(
        String id,
        String name,
        @Nullable Boolean leaf,
        @Nullable String parentId) {

    /** Map one generated Layer-1 event category to the public record. */
    static CategoryEventCategory from(CategoryEventBaseCategoryRaw raw) {
        return new CategoryEventCategory(
                raw.getId(), raw.getName(), raw.getLeaf(), parentId(raw.getParent()));
    }

    private static @Nullable String parentId(@Nullable CategoryEventBaseCategoryParentRaw parent) {
        return parent == null ? null : parent.getId();
    }
}
