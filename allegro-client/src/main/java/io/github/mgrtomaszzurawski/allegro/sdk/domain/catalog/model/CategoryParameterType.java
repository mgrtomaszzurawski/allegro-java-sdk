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
     * A parameter type this SDK version does not model. Future Allegro parameter
     * types map here so consumers never break on an unrecognised value — for
     * such a parameter {@link CategoryParameter#restrictions()} is {@code null}
     * and {@link CategoryParameter#dictionary()} is empty.
     */
    OTHER
}
