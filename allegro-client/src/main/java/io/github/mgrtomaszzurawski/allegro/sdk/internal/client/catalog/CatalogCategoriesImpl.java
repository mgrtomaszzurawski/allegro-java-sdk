/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoriesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogCategories;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link CatalogCategories} facade. Both list
 * methods hit {@code GET /sale/categories}; {@link #childrenOf(String)} adds the
 * {@code parent.id} filter, {@link #roots()} omits it.
 *
 * @since 0.2.0
 */
public final class CatalogCategoriesImpl implements CatalogCategories {

    private static final String OP_GET_CATEGORY = "get category";
    private static final String OP_ROOT_CATEGORIES = "get root categories";
    private static final String OP_CHILD_CATEGORIES = "get child categories";
    private static final String PARAM_PARENT_ID = "parent.id";

    private final HttpSupport http;

    public CatalogCategoriesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Category get(String categoryId) {
        String path = ApiPaths.subPath(ApiPaths.CATEGORIES, categoryId);
        return Category.from(http.request(OP_GET_CATEGORY).get(path).fetch(CategoryDtoRaw.class));
    }

    @Override
    public List<Category> roots() {
        return categories(Query.create(), OP_ROOT_CATEGORIES);
    }

    @Override
    public List<Category> childrenOf(String parentCategoryId) {
        return categories(Query.create().add(PARAM_PARENT_ID, parentCategoryId),
                OP_CHILD_CATEGORIES);
    }

    private List<Category> categories(Query query, String operationName) {
        CategoriesDtoRaw response = http.request(operationName)
                .get(ApiPaths.CATEGORIES)
                .query(query)
                .fetch(CategoriesDtoRaw.class);
        List<CategoryDtoRaw> rawCategories = response.getCategories();
        if (rawCategories == null) {
            return List.of();
        }
        return rawCategories.stream().map(Category::from).toList();
    }
}
