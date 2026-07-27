/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductParameterRaw;
import java.util.List;
import java.util.Objects;

/**
 * One parameter value carried by a product proposal ({@code catalog().products().propose(...)}
 * / {@code proposeChange(...)}). A parameter is identified by its {@code id} (from the
 * category's product parameters — {@code catalog().products().parametersIn(categoryId)}) and
 * supplies either dictionary value ids or free-text values, matching the parameter's kind.
 *
 * @param id the parameter id from the category's product schema
 * @param valuesIds dictionary value ids for a dictionary parameter (empty for a free-text one)
 * @param values free-text values for a free-text parameter (empty for a dictionary one)
 * @since 0.2.0
 */
public record ProductProposalParameter(String id, List<String> valuesIds, List<String> values) {

    /** Defensively copies the value lists and rejects a null id. */
    public ProductProposalParameter {
        Objects.requireNonNull(id, "id");
        valuesIds = valuesIds == null ? List.of() : List.copyOf(valuesIds);
        values = values == null ? List.of() : List.copyOf(values);
    }

    /** A dictionary parameter with the given value ids. */
    public static ProductProposalParameter ofValueIds(String id, String... valuesIds) {
        return new ProductProposalParameter(id, List.of(valuesIds), List.of());
    }

    /** A free-text parameter with the given values. */
    public static ProductProposalParameter ofValues(String id, String... values) {
        return new ProductProposalParameter(id, List.of(), List.of(values));
    }

    /** Project onto the generated Layer-1 request DTO. */
    public ProductParameterRaw toRaw() {
        ProductParameterRaw raw = new ProductParameterRaw();
        raw.setId(id);
        if (!valuesIds.isEmpty()) {
            raw.setValuesIds(valuesIds);
        }
        if (!values.isEmpty()) {
            raw.setValues(values);
        }
        return raw;
    }
}
