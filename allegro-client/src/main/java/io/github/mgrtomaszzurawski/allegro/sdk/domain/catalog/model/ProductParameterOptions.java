/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductParameterDtoOptionsRaw;

/**
 * Boolean traits of a {@link ProductParameterValue}: whether the parameter
 * identifies the product, is a GTIN, is trusted, and is AI co-created content.
 *
 * @param identifiesProduct whether this parameter identifies the product
 * @param gtin whether this parameter is a GTIN
 * @param trusted whether this parameter's value is trusted
 * @param aiCoCreated whether this parameter's value is AI co-created
 *
 * @since 0.4.0
 */
public record ProductParameterOptions(
        boolean identifiesProduct,
        boolean gtin,
        boolean trusted,
        boolean aiCoCreated) {

    private static final ProductParameterOptions NONE =
            new ProductParameterOptions(false, false, false, false);

    /** Map the generated Layer-1 DTO; absent options map to all-false. */
    public static ProductParameterOptions from(ProductParameterDtoOptionsRaw raw) {
        if (raw == null) {
            return NONE;
        }
        return new ProductParameterOptions(
                Boolean.TRUE.equals(raw.getIdentifiesProduct()),
                Boolean.TRUE.equals(raw.getIsGTIN()),
                Boolean.TRUE.equals(raw.getIsTrusted()),
                Boolean.TRUE.equals(raw.getIsAiCoCreated()));
    }
}
