/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibilitySuggestionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductGroupsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.CompatibleProductsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibilityList;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleProductGroup;
import java.util.List;
import java.util.stream.Stream;

/**
 * Vehicle/part compatibility lists — reached via {@code catalog().compatibility()}.
 *
 * <p>Some categories (car parts and accessories) let an offer carry a
 * compatibility list: the set of vehicles or products the item fits. This
 * sub-facade reads Allegro's reference data for building such lists.
 *
 * @since 0.2.0
 */
public interface Compatibility {

    /**
     * Lists the categories in which a compatibility list is supported, each with
     * how its items are supplied and the bounds on a free-text list.
     *
     * @return the supported categories; empty when none are returned
     */
    List<CompatibleCategory> supportedCategories();

    /**
     * Suggests the compatibility list for an offer or a product — the same hint the
     * sell form offers when classifying a car part.
     *
     * @param request the target, built via {@link CompatibilitySuggestionRequest}
     *     (which validates the offer-xor-product invariant fail-fast at build time)
     * @return the suggested list (manual or product-based)
     */
    CompatibilityList suggestionsFor(CompatibilitySuggestionRequest request);

    /**
     * Searches Allegro's compatible-products database — the {@code ID}-typed source
     * a car-parts offer picks its compatibility list from. Paginated lazily by
     * offset; a phrase search returns all matches at once.
     *
     * @param filter the search criteria (a {@code type} is required)
     * @return a lazy stream of matching products
     */
    Stream<CompatibleProduct> products(CompatibleProductsFilter filter);

    /**
     * Lists the groups compatible products are organized into — the coarse
     * dimension (e.g. vehicle make/model) whose id narrows a {@link
     * #products(CompatibleProductsFilter) products} search. Paginated lazily by
     * offset.
     *
     * @param filter the search criteria (a {@code type} is required)
     * @return a lazy stream of matching groups
     */
    Stream<CompatibleProductGroup> productGroups(CompatibleProductGroupsFilter filter);
}
