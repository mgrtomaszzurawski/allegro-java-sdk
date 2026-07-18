/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryProductParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DictionaryCategoryProductParameterAllOfDictionaryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DictionaryCategoryProductParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FloatCategoryProductParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.IntegerCategoryProductParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StringCategoryProductParameterRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A parameter a <em>product</em> in a category expects — reached via
 * {@code catalog().products().parametersIn(categoryId)}. This is the product-side
 * counterpart of {@link CategoryParameter} (which describes the parameters an
 * <em>offer</em> expects): the two share the value-type taxonomy
 * ({@link CategoryParameterType}), the flattened {@link ParameterRestrictions},
 * and the dictionary {@link DictionaryValue}s, but a product parameter carries
 * neither an offer's {@code requiredForProduct} flag nor the offer-only display
 * {@link CategoryParameterOptions} — so those are modelled only on
 * {@link CategoryParameter}.
 *
 * <p>The concrete shape depends on {@link #type()}: dictionary parameters carry a
 * {@link #dictionary()} of selectable values; numeric and string parameters carry
 * {@link #restrictions()} instead. Components that do not apply are {@code null}
 * or empty. The conditional-requirement metadata ({@code requiredIf}/
 * {@code displayedIf}/former ids) the wire also carries is a deferred projection,
 * matching {@link CategoryParameter}'s current depth.
 *
 * @param id the parameter id — use it to set the parameter's value on a product
 * @param name the parameter display name, localized to the marketplace default
 * @param type the value type, which selects the applicable restrictions
 * @param required whether a product must set this parameter to be created
 * @param unit the unit values are expressed in (e.g. {@code cm}), or {@code null}
 * @param restrictions the value constraints for the parameter's type, or
 *     {@code null} when none apply
 * @param dictionary the selectable values (dictionary parameters only; empty for
 *     every other type)
 *
 * @since 0.2.0
 */
public record ProductParameter(
        String id,
        String name,
        CategoryParameterType type,
        boolean required,
        @Nullable String unit,
        @Nullable ParameterRestrictions restrictions,
        List<DictionaryValue> dictionary) {

    public ProductParameter {
        dictionary = dictionary == null ? List.of() : List.copyOf(dictionary);
    }

    /**
     * Map a generated Layer-1 product-parameter DTO — of any concrete subtype — to
     * the public record, flattening the polymorphic restrictions and dictionary.
     */
    public static ProductParameter from(CategoryProductParameterRaw raw) {
        CategoryParameterType type;
        ParameterRestrictions restrictions;
        List<DictionaryValue> dictionary;
        if (raw instanceof DictionaryCategoryProductParameterRaw dictionaryParam) {
            type = CategoryParameterType.DICTIONARY;
            restrictions = ParameterRestrictions.fromDictionary(dictionaryParam.getRestrictions());
            dictionary = dictionaryOf(dictionaryParam.getDictionary());
        } else if (raw instanceof FloatCategoryProductParameterRaw floatParam) {
            type = CategoryParameterType.FLOAT;
            restrictions = ParameterRestrictions.fromFloat(floatParam.getRestrictions());
            dictionary = List.of();
        } else if (raw instanceof IntegerCategoryProductParameterRaw integerParam) {
            type = CategoryParameterType.INTEGER;
            restrictions = ParameterRestrictions.fromInteger(integerParam.getRestrictions());
            dictionary = List.of();
        } else if (raw instanceof StringCategoryProductParameterRaw stringParam) {
            type = CategoryParameterType.STRING;
            restrictions = ParameterRestrictions.fromString(stringParam.getRestrictions());
            dictionary = List.of();
        } else {
            // A base/unmodelled parameter type: the core UnknownSubtypeToBaseHandler
            // resolves an unknown discriminator to this base (the Raw has no
            // defaultImpl), so an unmodelled type reaches here and degrades to OTHER
            // rather than failing the read. See CategoryParameterType.OTHER.
            type = CategoryParameterType.OTHER;
            restrictions = null;
            dictionary = List.of();
        }
        return new ProductParameter(
                raw.getId(),
                raw.getName(),
                type,
                Boolean.TRUE.equals(raw.getRequired()),
                raw.getUnit(),
                restrictions,
                dictionary);
    }

    private static List<DictionaryValue> dictionaryOf(
            @Nullable List<DictionaryCategoryProductParameterAllOfDictionaryRaw> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(DictionaryValue::from).toList();
    }
}
