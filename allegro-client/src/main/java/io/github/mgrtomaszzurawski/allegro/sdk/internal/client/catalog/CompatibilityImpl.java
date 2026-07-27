/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListProductBasedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListTextItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsGroupsDtoGroupsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsGroupsDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsListDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.Compatibility;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibilitySuggestionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductGroupsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityList;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProductGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Compatibility} facade.
 * {@link #supportedCategories()} hits
 * {@code GET /sale/compatibility-list/supported-categories};
 * {@link #suggestionsFor} hits {@code GET /sale/compatibility-list-suggestions};
 * {@link #products} hits {@code GET /sale/compatible-products} and
 * {@link #productGroups} hits {@code GET /sale/compatible-products/groups}, both
 * offset-paginated as lazy streams.
 *
 * <p>The suggestions response is a discriminated {@code MANUAL}/{@code PRODUCT_BASED}
 * body whose generated subtypes do NOT share a common base (an OpenAPI-generator
 * quirk that makes the base undeserializable for {@code MANUAL}). It is therefore
 * fetched as a tree and resolved to the concrete subtype by its {@code type}
 * discriminator. A {@code MANUAL} list's items are a discriminated {@code oneOf}
 * ({@code ID}/{@code TEXT}) that the strict-oneOf module cannot disambiguate
 * structurally (an {@code ID} item's only distinguishing field is a non-enforced
 * required {@code id}), so each item is likewise resolved from the tree by its own
 * {@code type} discriminator rather than through the generated wrapper.
 *
 * @since 0.2.0
 */
public final class CompatibilityImpl implements Compatibility {

    private static final int PAGE_SIZE = 100;

    private static final String OP_SUPPORTED_CATEGORIES = "get compatibility supported categories";
    private static final String OP_SUGGESTIONS = "get compatibility list suggestion";
    private static final String OP_COMPATIBLE_PRODUCTS = "get compatible products";
    private static final String OP_COMPATIBLE_PRODUCT_GROUPS = "get compatible product groups";

    private static final String PARAM_OFFER_ID = "offer.id";
    private static final String PARAM_PRODUCT_ID = "product.id";
    private static final String PARAM_LANGUAGE = "language";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_GROUP_ID = "group.id";
    private static final String PARAM_TECDOC_KTYP = "tecdoc.kTypNr";
    private static final String PARAM_TECDOC_NTYP = "tecdoc.nTypNr";
    private static final String PARAM_PHRASE = "phrase";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_OFFSET = "offset";

    private static final String TYPE_FIELD = "type";
    private static final String ITEMS_FIELD = "items";
    private static final String TYPE_MANUAL = "MANUAL";
    private static final String TYPE_PRODUCT_BASED = "PRODUCT_BASED";
    private static final String ITEM_TYPE_ID = "ID";
    private static final String ITEM_TYPE_TEXT = "TEXT";
    private static final String ERR_REQUEST_NULL = "request must not be null";
    private static final String ERR_DESERIALIZE = "failed to map compatibility suggestion response";
    private static final String ERR_FILTER_NULL = "filter must not be null";

    private final HttpRuntime runtime;
    private final HttpSupport http;

    public CompatibilityImpl(HttpRuntime runtime) {
        this.runtime = runtime;
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
    public CompatibilityList suggestionsFor(CompatibilitySuggestionRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        Query query = Query.create()
                .add(PARAM_OFFER_ID, request.offerId())
                .add(PARAM_PRODUCT_ID, request.productId())
                .add(PARAM_LANGUAGE, request.language());
        JsonNode tree = http.request(OP_SUGGESTIONS)
                .get(ApiPaths.COMPATIBILITY_SUGGESTIONS)
                .query(query)
                .fetch(JsonNode.class);
        try {
            return toCompatibilityList(tree);
        } catch (JsonProcessingException ex) {
            throw new AllegroServerException(ERR_DESERIALIZE, ex);
        }
    }

    private CompatibilityList toCompatibilityList(JsonNode tree) throws JsonProcessingException {
        String type = tree.path(TYPE_FIELD).asText(null);
        if (TYPE_MANUAL.equals(type)) {
            return CompatibilityList.manual(manualItems(tree.path(ITEMS_FIELD)));
        }
        if (TYPE_PRODUCT_BASED.equals(type)) {
            return CompatibilityList.fromProductBased(
                    runtime.objectMapper().treeToValue(tree, CompatibilityListProductBasedRaw.class));
        }
        // A list type Allegro introduced after this SDK version — expose it as
        // UNKNOWN rather than failing the read (forward compatibility).
        return CompatibilityList.unknown();
    }

    private List<CompatibilityItem> manualItems(JsonNode itemsNode) throws JsonProcessingException {
        List<CompatibilityItem> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                items.add(manualItem(itemNode));
            }
        }
        return items;
    }

    private CompatibilityItem manualItem(JsonNode itemNode) throws JsonProcessingException {
        String itemType = itemNode.path(TYPE_FIELD).asText(null);
        ObjectMapper mapper = runtime.objectMapper();
        if (ITEM_TYPE_ID.equals(itemType)) {
            return CompatibilityItem.fromIdItem(
                    mapper.treeToValue(itemNode, CompatibilityListIdItemRaw.class));
        }
        if (ITEM_TYPE_TEXT.equals(itemType)) {
            return CompatibilityItem.fromTextItem(
                    mapper.treeToValue(itemNode, CompatibilityListTextItemRaw.class));
        }
        // An item variant this SDK version does not model — degrade the item.
        return CompatibilityItem.unknownItem();
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
        // A phrase search returns every match on the first page (Allegro ignores
        // offset/limit when phrase is set), so it never advances — guarding against
        // a non-advancing-offset fetch loop.
        boolean more = filter.phrase() == null
                && hasMoreByOffset(offset, items.size(), response.getTotalCount());
        return new PagedSpliterator.Page<>(items, more);
    }

    private PagedSpliterator.Page<CompatibleProductGroup> fetchProductGroups(
            CompatibleProductGroupsFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        // The groups endpoint takes only type + offset/limit — no phrase narrowing.
        Query query = Query.create()
                .add(PARAM_TYPE, filter.type())
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
                items, hasMoreByOffset(offset, items.size(), response.getTotalCount()));
    }

    /** Advance while the offset consumed so far is below the server's reported total. */
    private static boolean hasMoreByOffset(int offset, int pageCount, @Nullable Integer totalCount) {
        return totalCount != null && (long) offset + pageCount < totalCount;
    }
}
