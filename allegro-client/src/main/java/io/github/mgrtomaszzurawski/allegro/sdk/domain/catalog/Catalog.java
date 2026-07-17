/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

/**
 * Product catalogue — reached via {@code AllegroClient.catalog()}.
 *
 * <p>The catalogue is Allegro's read-only reference data: the category tree and
 * its parameters, the product database, and vehicle/part compatibility lists.
 * It is what an offer is classified and described against.
 *
 * <p>Starter slice of bucket E (catalog-products): only {@link #categories()}
 * ships first, as the end-to-end proof of the read path. {@code products()} and
 * {@code compatibility()} follow in the same bucket per the task-division plan.
 *
 * @since 0.2.0
 */
public interface Catalog {

    /**
     * The Allegro category tree and per-category parameters.
     *
     * @return the categories sub-facade
     */
    CatalogCategories categories();
}
