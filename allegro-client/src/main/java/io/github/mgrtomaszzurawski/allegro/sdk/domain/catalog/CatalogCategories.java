/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CategoryEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategorySuggestion;
import java.util.List;
import java.util.stream.Stream;

/**
 * The Allegro category tree — reached via {@code AllegroClient.catalog().categories()}.
 *
 * <p>Categories form a tree: {@link #roots()} returns the top-level nodes and
 * {@link #childrenOf(String)} walks down one level at a time. Only leaf
 * categories (see {@link Category#leaf()}) can hold offers.
 *
 * @since 0.2.0
 */
public interface CatalogCategories {

    /**
     * A single category by its identifier.
     *
     * @param categoryId the category identifier (an integer- or UUID-shaped
     *     string; treat it as opaque)
     * @return the category
     */
    Category get(String categoryId);

    /**
     * The top-level categories (those with no parent).
     *
     * @return the root categories, in the order Allegro returns them; never
     *     {@code null}, possibly empty
     */
    List<Category> roots();

    /**
     * The direct children of a category.
     *
     * @param parentCategoryId the parent category identifier
     * @return the immediate child categories, in the order Allegro returns them;
     *     never {@code null}, empty when the parent is a leaf
     */
    List<Category> childrenOf(String parentCategoryId);

    /**
     * The parameters a category expects on the offers and products classified
     * under it — their types, value restrictions and dictionaries.
     *
     * @param categoryId the category identifier
     * @return the category's parameters, in the order Allegro returns them;
     *     never {@code null}, empty when the category defines none
     */
    List<CategoryParameter> parameters(String categoryId);

    /**
     * Categories whose names best match a product or offer name — the same
     * suggestion Allegro's sell form makes for a title.
     *
     * @param productName the product or offer name to match against
     * @return the matching categories, best match first; never {@code null},
     *     possibly empty
     */
    List<CategorySuggestion> suggest(String productName);

    /**
     * Streams changes to the category tree — categories created, deleted, moved or
     * renamed — as a lazy feed. The stream follows Allegro's {@code from} cursor
     * internally (each event's id is the cursor); a bounded consumer only fetches the
     * pages it needs.
     *
     * @param filter which change kinds to include and where to resume from
     * @return a lazy stream of category changes, oldest first
     */
    Stream<CategoryEvent> streamChanges(CategoryEventFilter filter);
}
