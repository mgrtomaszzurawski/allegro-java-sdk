/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DictionaryCategoryParameterAllOfDictionaryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DictionaryCategoryProductParameterAllOfDictionaryRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One selectable value of a dictionary {@link CategoryParameter} or
 * {@link ProductParameter}.
 *
 * @param id the value id — use it to set this value on an offer or product
 * @param value the human-readable label (in Polish; per-value names are not
 *     localized)
 * @param dependsOnValueIds ids of values from the parameter named by
 *     {@link CategoryParameterOptions#dependsOnParameterId()} that this value may
 *     be combined with; empty when it may be combined with any of them.
 *     Always empty for a {@link ProductParameter}'s values — the product-side
 *     dictionary carries no combination dependencies.
 *
 * @since 0.2.0
 */
public record DictionaryValue(String id, String value, List<String> dependsOnValueIds) {

    public DictionaryValue {
        dependsOnValueIds = dependsOnValueIds == null ? List.of() : List.copyOf(dependsOnValueIds);
    }

    static DictionaryValue from(DictionaryCategoryParameterAllOfDictionaryRaw raw) {
        return new DictionaryValue(raw.getId(), raw.getValue(), raw.getDependsOnValueIds());
    }

    static DictionaryValue from(DictionaryCategoryProductParameterAllOfDictionaryRaw raw) {
        // The product-side dictionary value has no dependsOnValueIds (a category-only
        // concept), so it is always an empty list here.
        return new DictionaryValue(raw.getId(), raw.getValue(), List.of());
    }

    static List<DictionaryValue> listFrom(
            @Nullable List<DictionaryCategoryParameterAllOfDictionaryRaw> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(DictionaryValue::from).toList();
    }
}
