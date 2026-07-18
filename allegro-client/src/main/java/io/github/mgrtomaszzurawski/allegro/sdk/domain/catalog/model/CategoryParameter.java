/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DictionaryCategoryParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FloatCategoryParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.IntegerCategoryParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StringCategoryParameterRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A parameter a category expects on its offers and products — reached via
 * {@code catalog().categories().parameters(categoryId)}.
 *
 * <p>The concrete shape depends on {@link #type()}: dictionary parameters carry
 * a {@link #dictionary()} of selectable values; numeric and string parameters
 * carry {@link #restrictions()} instead. Components that do not apply to a
 * parameter's type are {@code null} or empty.
 *
 * @param id the parameter id — use it to set the parameter's value on an offer
 *     or product
 * @param name the parameter display name, localized to the marketplace default
 * @param type the value type, which selects the applicable restrictions
 * @param required whether an offer must set this parameter to be published
 * @param requiredForProduct whether a product must set this parameter to be
 *     created
 * @param unit the unit values are expressed in (e.g. {@code cm}), or {@code null}
 * @param options extra behaviour flags, or {@code null} when none apply
 * @param restrictions the value constraints for the parameter's type, or
 *     {@code null} when none apply
 * @param dictionary the selectable values (dictionary parameters only; empty for
 *     every other type)
 *
 * @since 0.2.0
 */
public record CategoryParameter(
        String id,
        String name,
        CategoryParameterType type,
        boolean required,
        boolean requiredForProduct,
        @Nullable String unit,
        @Nullable CategoryParameterOptions options,
        @Nullable ParameterRestrictions restrictions,
        List<DictionaryValue> dictionary) {

    public CategoryParameter {
        dictionary = dictionary == null ? List.of() : List.copyOf(dictionary);
    }

    /**
     * Map a generated Layer-1 parameter DTO — of any concrete subtype — to the
     * public record, flattening the polymorphic restrictions and dictionary.
     */
    public static CategoryParameter from(CategoryParameterRaw raw) {
        CategoryParameterType type;
        ParameterRestrictions restrictions;
        List<DictionaryValue> dictionary;
        if (raw instanceof DictionaryCategoryParameterRaw dictionaryParam) {
            type = CategoryParameterType.DICTIONARY;
            restrictions = ParameterRestrictions.fromDictionary(dictionaryParam.getRestrictions());
            dictionary = DictionaryValue.listFrom(dictionaryParam.getDictionary());
        } else if (raw instanceof FloatCategoryParameterRaw floatParam) {
            type = CategoryParameterType.FLOAT;
            restrictions = ParameterRestrictions.fromFloat(floatParam.getRestrictions());
            dictionary = List.of();
        } else if (raw instanceof IntegerCategoryParameterRaw integerParam) {
            type = CategoryParameterType.INTEGER;
            restrictions = ParameterRestrictions.fromInteger(integerParam.getRestrictions());
            dictionary = List.of();
        } else if (raw instanceof StringCategoryParameterRaw stringParam) {
            type = CategoryParameterType.STRING;
            restrictions = ParameterRestrictions.fromString(stringParam.getRestrictions());
            dictionary = List.of();
        } else {
            // Defensive default for a base/unmodelled parameter type. The standard
            // wire path does not reach this yet: Jackson rejects an unknown
            // discriminator (the Raw base has no defaultImpl) before from() runs.
            // See CategoryParameterType.OTHER.
            type = CategoryParameterType.OTHER;
            restrictions = null;
            dictionary = List.of();
        }
        return new CategoryParameter(
                raw.getId(),
                raw.getName(),
                type,
                Boolean.TRUE.equals(raw.getRequired()),
                Boolean.TRUE.equals(raw.getRequiredForProduct()),
                raw.getUnit(),
                CategoryParameterOptions.from(raw.getOptions()),
                restrictions,
                dictionary);
    }
}
