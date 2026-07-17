/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import java.util.List;

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
}
