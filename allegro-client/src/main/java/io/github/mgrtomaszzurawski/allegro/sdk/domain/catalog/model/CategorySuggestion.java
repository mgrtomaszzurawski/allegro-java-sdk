/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategorySuggestionCategoryNodeRaw;
import org.jspecify.annotations.Nullable;

/**
 * A category matched by {@code catalog().categories().suggest(productName)},
 * together with its position in the tree.
 *
 * <p>Walk {@link #parent()} up to the root to build the breadcrumb of the
 * matched category.
 *
 * @param id the matched category id
 * @param name the matched category name
 * @param parent the parent category, or {@code null} when the match is itself a
 *     root category
 *
 * @since 0.2.0
 */
public record CategorySuggestion(String id, String name, @Nullable CategorySuggestion parent) {

    /**
     * Map a generated Layer-1 suggestion node to the public record, recursively
     * mapping its parent chain up to the root.
     */
    public static CategorySuggestion from(CategorySuggestionCategoryNodeRaw raw) {
        CategorySuggestionCategoryNodeRaw parentRaw = raw.getParent();
        return new CategorySuggestion(
                raw.getId(),
                raw.getName(),
                parentRaw == null ? null : from(parentRaw));
    }
}
