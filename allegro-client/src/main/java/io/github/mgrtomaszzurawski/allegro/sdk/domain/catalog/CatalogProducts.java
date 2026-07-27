/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductChangeProposalRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductProposalRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Product;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductChangeProposal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductProposal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import java.util.List;
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

    /**
     * The parameters a product in a category expects, so a caller can learn a
     * category's product schema before building a product for it. This is the
     * product-side counterpart of
     * {@code catalog().categories().parameters(categoryId)} (which lists the
     * parameters an <em>offer</em> expects).
     *
     * @param categoryId the leaf category id (e.g. from a category-tree walk or a
     *     {@code categories().suggest(...)} match)
     * @return the product parameters, in the order Allegro returns them; empty
     *     when the category defines none
     */
    List<ProductParameter> parametersIn(String categoryId);

    /**
     * Propose a new catalogue product Allegro does not yet carry. Allegro moderates
     * the proposal; the returned {@link ProductProposal} carries the assigned id and
     * its {@code PROPOSED}/{@code LISTED} status.
     *
     * @param request the product to propose (name and category required)
     * @return the created proposal
     */
    ProductProposal propose(ProductProposalRequest request);

    /**
     * Propose a change to an existing catalogue product. Allegro moderates the change;
     * read its state back later with {@link #changeProposal(String)}.
     *
     * @param productId the product to propose changes for
     * @param request the corrected product picture (name required)
     * @return the created change proposal, with its assigned id
     */
    ProductChangeProposal proposeChange(String productId, ProductChangeProposalRequest request);

    /**
     * Read a product change proposal by its id — the proposed fields and how Allegro
     * resolved them.
     *
     * @param changeProposalId the change-proposal id (e.g. from
     *     {@link #proposeChange(String, ProductChangeProposalRequest)})
     * @return the change proposal
     */
    ProductChangeProposal changeProposal(String changeProposalId);
}
