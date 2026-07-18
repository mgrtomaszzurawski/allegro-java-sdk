/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.Catalog;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogCategories;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogProducts;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Root wiring behind the {@link Catalog} facade — holds the sub-facades and
 * hands each the shared runtime.
 *
 * @since 0.2.0
 */
public final class CatalogImpl implements Catalog {

    private final CatalogCategories categories;
    private final CatalogProducts products;

    public CatalogImpl(HttpRuntime runtime) {
        this.categories = new CatalogCategoriesImpl(runtime);
        this.products = new CatalogProductsImpl(runtime);
    }

    @Override
    public CatalogCategories categories() {
        return categories;
    }

    @Override
    public CatalogProducts products() {
        return products;
    }
}
