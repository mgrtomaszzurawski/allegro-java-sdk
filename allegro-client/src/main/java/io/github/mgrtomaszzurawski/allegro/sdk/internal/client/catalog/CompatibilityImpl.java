/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.catalog;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.Compatibility;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link Compatibility} facade.
 * {@link #supportedCategories()} hits
 * {@code GET /sale/compatibility-list/supported-categories}.
 *
 * @since 0.2.0
 */
public final class CompatibilityImpl implements Compatibility {

    private static final String OP_SUPPORTED_CATEGORIES = "get compatibility supported categories";

    private final HttpSupport http;

    public CompatibilityImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<CompatibleCategory> supportedCategories() {
        CompatibilityListSupportedCategoriesDtoRaw response = http.request(OP_SUPPORTED_CATEGORIES)
                .get(ApiPaths.COMPATIBILITY_SUPPORTED_CATEGORIES)
                .fetch(CompatibilityListSupportedCategoriesDtoRaw.class);
        List<CompatibilityListSupportedCategoriesDtoSupportedCategoriesInnerRaw> rawCategories =
                response.getSupportedCategories();
        if (rawCategories == null) {
            return List.of();
        }
        return rawCategories.stream().map(CompatibleCategory::from).toList();
    }
}
