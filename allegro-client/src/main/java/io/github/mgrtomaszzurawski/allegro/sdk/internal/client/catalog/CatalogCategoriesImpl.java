/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoriesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryBaseEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryEventsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParameterListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategorySuggestionCategoryNodeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategorySuggestionResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogCategories;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CategoryEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEventType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategorySuggestion;
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
 * Endpoint wrapper behind the {@link CatalogCategories} facade. {@link #roots()}
 * and {@link #childrenOf(String)} both hit {@code GET /sale/categories} (the
 * latter adding the {@code parent.id} filter); {@link #get(String)} and
 * {@link #parameters(String)} address a category by id, and {@link #suggest(String)}
 * matches by name via {@code GET /sale/matching-categories}.
 *
 * @since 0.2.0
 */
public final class CatalogCategoriesImpl implements CatalogCategories {

    private static final String OP_GET_CATEGORY = "get category";
    private static final String OP_ROOT_CATEGORIES = "get root categories";
    private static final String OP_CHILD_CATEGORIES = "get child categories";
    private static final String OP_CATEGORY_PARAMETERS = "get category parameters";
    private static final String OP_SUGGEST_CATEGORIES = "suggest categories";
    private static final String OP_CATEGORY_EVENTS = "stream category events";
    private static final int EVENTS_PAGE_SIZE = 100;
    private static final String PARAM_PARENT_ID = "parent.id";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_FROM = "from";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_TYPE = "type";
    private static final String ERR_CATEGORY_ID_NULL = "categoryId must not be null";
    private static final String ERR_PARENT_ID_NULL = "parentCategoryId must not be null";
    private static final String ERR_PRODUCT_NAME_NULL = "productName must not be null";
    private static final String ERR_FILTER_NULL = "filter must not be null";

    private final HttpSupport http;

    public CatalogCategoriesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Category get(String categoryId) {
        Objects.requireNonNull(categoryId, ERR_CATEGORY_ID_NULL);
        String path = ApiPaths.subPath(ApiPaths.CATEGORIES, categoryId);
        return Category.from(http.request(OP_GET_CATEGORY).get(path).fetch(CategoryDtoRaw.class));
    }

    @Override
    public List<Category> roots() {
        return categories(Query.create(), OP_ROOT_CATEGORIES);
    }

    @Override
    public List<Category> childrenOf(String parentCategoryId) {
        // Fail fast: without this, a null id would be dropped by Query's null-skip
        // and the call would silently degrade into roots() instead of erroring.
        Objects.requireNonNull(parentCategoryId, ERR_PARENT_ID_NULL);
        return categories(Query.create().add(PARAM_PARENT_ID, parentCategoryId),
                OP_CHILD_CATEGORIES);
    }

    @Override
    public List<CategoryParameter> parameters(String categoryId) {
        Objects.requireNonNull(categoryId, ERR_CATEGORY_ID_NULL);
        String path = ApiPaths.subPath(
                ApiPaths.CATEGORIES, categoryId, ApiPaths.CATEGORY_PARAMETERS_SEGMENT);
        CategoryParameterListRaw response = http.request(OP_CATEGORY_PARAMETERS)
                .get(path)
                .fetch(CategoryParameterListRaw.class);
        List<CategoryParameterRaw> rawParameters = response.getParameters();
        if (rawParameters == null) {
            return List.of();
        }
        return rawParameters.stream().map(CategoryParameter::from).toList();
    }

    @Override
    public List<CategorySuggestion> suggest(String productName) {
        // Fail fast: the name is the required query parameter; a null would be
        // dropped by Query's null-skip and match every category instead.
        Objects.requireNonNull(productName, ERR_PRODUCT_NAME_NULL);
        CategorySuggestionResponseRaw response = http.request(OP_SUGGEST_CATEGORIES)
                .get(ApiPaths.MATCHING_CATEGORIES)
                .query(Query.create().add(PARAM_NAME, productName))
                .fetch(CategorySuggestionResponseRaw.class);
        List<CategorySuggestionCategoryNodeRaw> rawMatches = response.getMatchingCategories();
        if (rawMatches == null) {
            return List.of();
        }
        return rawMatches.stream().map(CategorySuggestion::from).toList();
    }

    @Override
    public Stream<CategoryEvent> streamChanges(CategoryEventFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        return PagedSpliterator.cursorStream(cursor -> fetchEventPage(filter, cursor));
    }

    private PagedSpliterator.CursorPage<CategoryEvent> fetchEventPage(
            CategoryEventFilter filter, @Nullable String cursor) {
        // The first page (cursor == null) starts from the filter's resume point;
        // later pages follow the cursor (the last event id of the previous page).
        String from = cursor != null ? cursor : filter.from();
        Query query = Query.create()
                .add(PARAM_FROM, from)
                .add(PARAM_LIMIT, EVENTS_PAGE_SIZE)
                .addAll(PARAM_TYPE, eventTypeValues(filter));
        CategoryEventsResponseRaw response = http.request(OP_CATEGORY_EVENTS)
                .get(ApiPaths.CATEGORY_EVENTS)
                .query(query)
                .fetch(CategoryEventsResponseRaw.class);
        List<CategoryBaseEventRaw> events = response.getEvents();
        List<CategoryEvent> items = events == null
                ? List.of()
                : events.stream().map(CategoryEvent::from).toList();
        // A full page means there may be more; advance the cursor to the last event id.
        String nextCursor =
                items.size() == EVENTS_PAGE_SIZE ? items.get(items.size() - 1).id() : null;
        return new PagedSpliterator.CursorPage<>(items, nextCursor);
    }

    private static List<String> eventTypeValues(CategoryEventFilter filter) {
        return filter.types().stream()
                .map(CategoryEventType::wireValue)
                .filter(Objects::nonNull)
                .toList();
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
