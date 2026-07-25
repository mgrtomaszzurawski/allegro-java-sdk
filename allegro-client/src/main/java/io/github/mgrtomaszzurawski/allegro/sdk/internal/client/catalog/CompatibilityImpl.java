/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsGroupsDtoGroupsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsGroupsDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsListDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.Compatibility;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductGroupsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProductGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Compatibility} facade.
 * {@link #supportedCategories()} hits
 * {@code GET /sale/compatibility-list/supported-categories}; {@link #products} hits
 * {@code GET /sale/compatible-products} and {@link #productGroups} hits
 * {@code GET /sale/compatible-products/groups}, both offset-paginated as lazy
 * streams.
 *
 * @since 0.2.0
 */
public final class CompatibilityImpl implements Compatibility {

    private static final int PAGE_SIZE = 100;

    private static final String OP_SUPPORTED_CATEGORIES = "get compatibility supported categories";
    private static final String OP_COMPATIBLE_PRODUCTS = "get compatible products";
    private static final String OP_COMPATIBLE_PRODUCT_GROUPS = "get compatible product groups";

    private static final String PARAM_TYPE = "type";
    private static final String PARAM_GROUP_ID = "group.id";
    private static final String PARAM_TECDOC_KTYP = "tecdoc.kTypNr";
    private static final String PARAM_TECDOC_NTYP = "tecdoc.nTypNr";
    private static final String PARAM_PHRASE = "phrase";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_OFFSET = "offset";

    private static final String ERR_FILTER_NULL = "filter must not be null";

    private final HttpSupport http;

    public CompatibilityImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<CompatibleCategory> supportedCategories() {
        CompatibilityListSupportedCategoriesDtoRaw response = http.request(OP_SUPPORTED_CATEGORIES)
                .get(ApiPaths.COMPATIBILITY_SUPPORTED_CATEGORIES)
                .fetch(CompatibilityListSupportedCategoriesDtoRaw.class);
        List<CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw> rawCategories =
                response.getSupportedCategories();
        if (rawCategories == null) {
            return List.of();
        }
        return rawCategories.stream().map(CompatibleCategory::from).toList();
    }

    @Override
    public Stream<CompatibleProduct> products(CompatibleProductsFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        return PagedSpliterator.stream(pageIndex -> fetchProducts(filter, pageIndex));
    }

    @Override
    public Stream<CompatibleProductGroup> productGroups(CompatibleProductGroupsFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        return PagedSpliterator.stream(pageIndex -> fetchProductGroups(filter, pageIndex));
    }

    private PagedSpliterator.Page<CompatibleProduct> fetchProducts(
            CompatibleProductsFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(PARAM_TYPE, filter.type())
                .add(PARAM_GROUP_ID, filter.groupId())
                .add(PARAM_TECDOC_KTYP, filter.tecdocKTypNr())
                .add(PARAM_TECDOC_NTYP, filter.tecdocNTypNr())
                .add(PARAM_PHRASE, filter.phrase())
                .add(PARAM_LIMIT, PAGE_SIZE)
                .add(PARAM_OFFSET, offset);
        CompatibleProductsListDtoRaw response = http.request(OP_COMPATIBLE_PRODUCTS)
                .get(ApiPaths.COMPATIBLE_PRODUCTS)
                .query(query)
                .fetch(CompatibleProductsListDtoRaw.class);
        List<CompatibleProductDtoRaw> raw = response.getCompatibleProducts();
        List<CompatibleProduct> items = raw == null
                ? List.of()
                : raw.stream().map(CompatibleProduct::from).toList();
        return new PagedSpliterator.Page<>(
                items, hasMore(filter.phrase(), offset, items.size(), response.getTotalCount()));
    }

    private PagedSpliterator.Page<CompatibleProductGroup> fetchProductGroups(
            CompatibleProductGroupsFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(PARAM_TYPE, filter.type())
                .add(PARAM_PHRASE, filter.phrase())
                .add(PARAM_LIMIT, PAGE_SIZE)
                .add(PARAM_OFFSET, offset);
        CompatibleProductsGroupsDtoRaw response = http.request(OP_COMPATIBLE_PRODUCT_GROUPS)
                .get(ApiPaths.COMPATIBLE_PRODUCTS_GROUPS)
                .query(query)
                .fetch(CompatibleProductsGroupsDtoRaw.class);
        List<CompatibleProductsGroupsDtoGroupsInnerRaw> raw = response.getGroups();
        List<CompatibleProductGroup> items = raw == null
                ? List.of()
                : raw.stream().map(CompatibleProductGroup::from).toList();
        return new PagedSpliterator.Page<>(
                items, hasMore(filter.phrase(), offset, items.size(), response.getTotalCount()));
    }

    /**
     * Whether another page should be fetched. A phrase search returns every match on
     * the first page (Allegro ignores offset/limit when {@code phrase} is set), so it
     * never advances — guarding against a non-advancing-offset fetch loop. Otherwise
     * advance while the offset consumed so far is below the reported total.
     */
    private static boolean hasMore(
            @Nullable String phrase, int offset, int pageCount, @Nullable Integer totalCount) {
        if (phrase != null || totalCount == null) {
            return false;
        }
        return (long) offset + pageCount < totalCount;
    }
}
