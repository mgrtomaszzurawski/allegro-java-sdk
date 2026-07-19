/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

/**
 * The kind of an {@link OfferParameter}, i.e. which value shape it carries. A
 * category dictates each parameter's kind; use {@link OfferParameter#kind()} to
 * branch on it rather than inspecting which value list is populated (on read a
 * dictionary parameter carries both its ids and their labels).
 *
 * @since 0.3.0
 */
public enum OfferParameterKind {

    /** A value chosen from the category's value dictionary (carried as {@code valuesIds}). */
    DICTIONARY,
    /** A free-form text value (carried as {@code values}). */
    FREE_TEXT,
    /** A numeric or date range (carried as a {@link ParameterRange}). */
    RANGE
}
