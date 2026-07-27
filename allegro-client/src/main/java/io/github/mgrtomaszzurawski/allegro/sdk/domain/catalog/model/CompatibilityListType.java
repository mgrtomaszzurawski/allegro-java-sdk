/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

/**
 * The kind of a {@link CompatibilityList}: whether its items are supplied by the
 * seller directly ({@link #MANUAL}) or derived from an associated product
 * ({@link #PRODUCT_BASED}).
 *
 * @since 0.2.0
 */
public enum CompatibilityListType {

    /**
     * The list is built from items the seller provides directly (offers not tied
     * to a product). Its items may be picked by id or entered as free text.
     */
    MANUAL,

    /**
     * The list is derived from the product the offer is associated with; its items
     * are a read-only text representation and must be included unchanged.
     */
    PRODUCT_BASED,

    /**
     * A list type this SDK release does not model yet — a forward-compat sentinel
     * for a variant Allegro introduced after this version (the polymorphic base
     * degraded past {@code MANUAL}/{@code PRODUCT_BASED}).
     */
    UNKNOWN
}
