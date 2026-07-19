/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerValidationRulesRaw;
import org.jspecify.annotations.Nullable;

/**
 * Bounds Allegro places on a {@link CompatibleCategory}'s free-text
 * compatibility list — meaningful only when the category's
 * {@link CompatibilityInputType} is {@link CompatibilityInputType#TEXT}.
 *
 * @param maxRows the maximum number of text rows the list may hold, or
 *     {@code null} when unbounded
 * @param maxCharactersPerLine the maximum characters allowed per row, or
 *     {@code null} when unbounded
 *
 * @since 0.2.0
 */
public record CompatibilityValidationRules(
        @Nullable Integer maxRows,
        @Nullable Integer maxCharactersPerLine) {

    /**
     * Map the generated Layer-1 DTO, or {@code null} when the category carries
     * no validation-rules block.
     */
    static @Nullable CompatibilityValidationRules from(
            @Nullable CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerValidationRulesRaw raw) {
        if (raw == null) {
            return null;
        }
        return new CompatibilityValidationRules(raw.getMaxRows(), raw.getMaxCharactersPerLine());
    }
}
