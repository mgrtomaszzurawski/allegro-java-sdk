/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductParameterDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A parameter value set on a {@link Product} — the parameter's id and name, its
 * value(s), and the unit they are expressed in.
 *
 * <p>This is the value carried by a product (e.g. {@code Pojemność = 128 GB}),
 * distinct from a {@link CategoryParameter}, which describes what a category
 * <em>permits</em>. For a dictionary parameter, {@link #values()} are the
 * (localized) display labels and {@link #valuesIds()} the stable ids to match
 * against a {@link DictionaryValue#id()}.
 *
 * <p>Range-type parameters (which carry {@code from}/{@code to} bounds rather
 * than discrete values) are not yet modelled here; their {@link #values()} may
 * be empty.
 *
 * @param id the parameter id (matches a {@link CategoryParameter#id()})
 * @param name the parameter display name
 * @param values the value label(s) set for this parameter, in order; never
 *     {@code null}, possibly empty
 * @param valuesIds the stable id(s) of the set value(s) (dictionary parameters),
 *     in order; never {@code null}, possibly empty
 * @param unit the unit the values are expressed in, or {@code null}
 *
 * @since 0.2.0
 */
public record ProductParameterValue(
        String id,
        String name,
        List<String> values,
        List<String> valuesIds,
        @Nullable String unit) {

    public ProductParameterValue {
        values = values == null ? List.of() : List.copyOf(values);
        valuesIds = valuesIds == null ? List.of() : List.copyOf(valuesIds);
    }

    /** Map a generated Layer-1 product-parameter DTO to the public record. */
    public static ProductParameterValue from(ProductParameterDtoRaw raw) {
        return new ProductParameterValue(
                raw.getId(), raw.getName(), raw.getValues(), raw.getValuesIds(), raw.getUnit());
    }
}
