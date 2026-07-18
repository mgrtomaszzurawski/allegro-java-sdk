/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParameterOptionsRaw;
import org.jspecify.annotations.Nullable;

/**
 * Extra behaviour flags on a {@link CategoryParameter}.
 *
 * @param ambiguousValueId the dictionary value id defined as ambiguous, or
 *     {@code null} (dictionary parameters only)
 * @param dependsOnParameterId id of the parameter whose selected value
 *     constrains this one, or {@code null} when it is independent
 * @param describesProduct whether the parameter is used to define products
 * @param customValuesEnabled whether a custom value may be added to a parameter
 *     that has an ambiguous value
 *
 * @since 0.2.0
 */
public record CategoryParameterOptions(
        @Nullable String ambiguousValueId,
        @Nullable String dependsOnParameterId,
        boolean describesProduct,
        boolean customValuesEnabled) {

    /**
     * Map the generated Layer-1 DTO, or {@code null} when the parameter carries
     * no options block.
     */
    static @Nullable CategoryParameterOptions from(@Nullable CategoryParameterOptionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new CategoryParameterOptions(
                raw.getAmbiguousValueId(),
                raw.getDependsOnParameterId(),
                Boolean.TRUE.equals(raw.getDescribesProduct()),
                Boolean.TRUE.equals(raw.getCustomValuesEnabled()));
    }
}
