/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw;
import org.jspecify.annotations.Nullable;

/**
 * A category in which Allegro supports a compatibility list — the vehicle/part
 * fitment list an offer can carry. Returned by
 * {@code catalog().compatibility().supportedCategories()}.
 *
 * @param categoryId the id of the category that supports a compatibility list
 * @param name the category's display name
 * @param itemsType what the list's items describe (e.g. the vehicle domain)
 * @param inputType how the list's items are supplied — by product {@code ID} or
 *     free {@code TEXT}
 * @param validationRules the bounds on a free-text list, or {@code null} when
 *     the category carries none (typically for {@link CompatibilityInputType#ID})
 *
 * @since 0.2.0
 */
public record CompatibleCategory(
        String categoryId,
        String name,
        String itemsType,
        CompatibilityInputType inputType,
        @Nullable CompatibilityValidationRules validationRules) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static CompatibleCategory from(
            CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw raw) {
        return new CompatibleCategory(
                raw.getCategoryId(),
                raw.getName(),
                raw.getItemsType(),
                CompatibilityInputType.from(raw.getInputType()),
                CompatibilityValidationRules.from(raw.getValidationRules()));
    }
}
