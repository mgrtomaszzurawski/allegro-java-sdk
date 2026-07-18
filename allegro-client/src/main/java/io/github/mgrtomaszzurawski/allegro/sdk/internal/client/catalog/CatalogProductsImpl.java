/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryProductParameterListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryProductParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetSaleProductsResponseNextPageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetSaleProductsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogProducts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Product;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator.CursorPage;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link CatalogProducts} facade. {@link #search}
 * pages {@code GET /sale/products} lazily by its opaque {@code page.id} cursor;
 * {@link #get(String)} reads one product ({@code GET /sale/products/{id}});
 * {@link #parametersIn(String)} lists a category's product parameters
 * ({@code GET /sale/categories/{id}/product-parameters}).
 *
 * @since 0.2.0
 */
public final class CatalogProductsImpl implements CatalogProducts {

    private static final String OP_SEARCH_PRODUCTS = "search products";
    private static final String PARAM_PHRASE = "phrase";
    private static final String PARAM_CATEGORY_ID = "category.id";
    private static final String PARAM_LANGUAGE = "language";
    private static final String PARAM_PAGE_ID = "page.id";
    private static final String OP_GET_PRODUCT = "get product";
    private static final String OP_PRODUCT_PARAMETERS = "get product parameters";
    private static final String ERR_REQUEST_NULL = "request must not be null";
    private static final String ERR_PRODUCT_ID_NULL = "productId must not be null";
    private static final String ERR_CATEGORY_ID_NULL = "categoryId must not be null";

    private final HttpSupport http;

    public CatalogProductsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<ProductSummary> search(ProductSearchRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return PagedSpliterator.cursorStream(cursor -> fetchPage(request, cursor));
    }

    @Override
    public Product get(String productId) {
        Objects.requireNonNull(productId, ERR_PRODUCT_ID_NULL);
        String path = ApiPaths.subPath(ApiPaths.PRODUCTS, productId);
        return Product.from(http.request(OP_GET_PRODUCT).get(path).fetch(SaleProductDtoRaw.class));
    }

    @Override
    public List<ProductParameter> parametersIn(String categoryId) {
        Objects.requireNonNull(categoryId, ERR_CATEGORY_ID_NULL);
        String path = ApiPaths.subPath(
                ApiPaths.CATEGORIES, categoryId, ApiPaths.PRODUCT_PARAMETERS_SEGMENT);
        CategoryProductParameterListRaw response = http.request(OP_PRODUCT_PARAMETERS)
                .get(path)
                .fetch(CategoryProductParameterListRaw.class);
        List<CategoryProductParameterRaw> rawParameters = response.getParameters();
        if (rawParameters == null) {
            return List.of();
        }
        return rawParameters.stream().map(ProductParameter::from).toList();
    }

    private CursorPage<ProductSummary> fetchPage(ProductSearchRequest request, @Nullable String pageId) {
        Query query = Query.create()
                .add(PARAM_PHRASE, request.phrase())
                .add(PARAM_CATEGORY_ID, request.categoryId())
                .add(PARAM_LANGUAGE, request.language())
                .add(PARAM_PAGE_ID, pageId);
        GetSaleProductsResponseRaw response = http.request(OP_SEARCH_PRODUCTS)
                .get(ApiPaths.PRODUCTS)
                .query(query)
                .fetch(GetSaleProductsResponseRaw.class);
        // `products` is a spec-required field (never null); trust the contract.
        List<ProductSummary> items = response.getProducts().stream()
                .map(ProductSummary::from).toList();
        GetSaleProductsResponseNextPageRaw nextPage = response.getNextPage();
        String nextCursor = nextPage == null ? null : nextPage.getId();
        return new CursorPage<>(items, nextCursor);
    }
}
