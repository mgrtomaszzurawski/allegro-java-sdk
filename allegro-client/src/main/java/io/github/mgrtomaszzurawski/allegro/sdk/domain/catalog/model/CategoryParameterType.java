/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

/**
 * The value type of a {@link CategoryParameter}. It determines how the
 * parameter's value is expressed on an offer or product and which
 * {@link ParameterRestrictions} components apply.
 *
 * @since 0.2.0
 */
public enum CategoryParameterType {

    /** Value(s) chosen from the fixed {@link CategoryParameter#dictionary()}. */
    DICTIONARY,

    /** A decimal number, optionally bounded and precision-limited. */
    FLOAT,

    /** An integer number, optionally bounded. */
    INTEGER,

    /** Free-form text, optionally length-limited. */
    STRING,

    /**
     * The mapper's default for a parameter whose concrete type is none of the
     * four above; {@link CategoryParameter#restrictions()} is then {@code null}
     * and {@link CategoryParameter#dictionary()} is empty.
     *
     * <p>Reachability: the Layer-1 parameter DTO declares no Jackson
     * {@code defaultImpl}, so an unknown {@code type} on the wire currently fails
     * deserialization (surfaced as an {@code AllegroServerException}) before the
     * mapper runs — it does not yet degrade to {@code OTHER}. Delivering that
     * degradation is a tracked core follow-up.
     */
    OTHER
}
