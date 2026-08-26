/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductsCategoryPathRaw;
import org.jspecify.annotations.Nullable;

/**
 * One node on a {@link Product}'s category path, from the root category down to the
 * category the product is classified under.
 *
 * @param id the category id, or {@code null}
 * @param name the category name, or {@code null}
 *
 * @since 0.4.0
 */
public record ProductCategoryPathElement(@Nullable String id, @Nullable String name) {

    /** Map the generated Layer-1 DTO. */
    public static ProductCategoryPathElement from(ProductsCategoryPathRaw raw) {
        return new ProductCategoryPathElement(raw.getId(), raw.getName());
    }
}
