/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Product;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import java.util.stream.Stream;

/**
 * The Allegro product database — reached via
 * {@code AllegroClient.catalog().products()}.
 *
 * <p>Products are the shared descriptions offers are built from. This facade
 * searches them; the full-product read and change proposals follow in the same
 * bucket.
 *
 * @since 0.2.0
 */
public interface CatalogProducts {

    /**
     * Search the product database, lazily. The returned stream fetches one page
     * at a time and follows Allegro's opaque {@code page.id} cursor
     * automatically, so a bounded consumer (e.g. {@code limit}) fetches only the
     * pages it needs.
     *
     * @param request the search criteria (a phrase, with an optional category filter)
     * @return a lazy stream of matching product summaries, best match first
     */
    Stream<ProductSummary> search(ProductSearchRequest request);

    /**
     * A single product by its id, with the parameter values that describe it.
     *
     * @param productId the product id (e.g. from a {@link ProductSummary#id()})
     * @return the full product
     */
    Product get(String productId);
}
