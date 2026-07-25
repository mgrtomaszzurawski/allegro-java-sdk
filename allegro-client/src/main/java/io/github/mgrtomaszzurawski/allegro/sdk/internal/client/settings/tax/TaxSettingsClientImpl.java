/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.tax;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryTaxSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model.TaxSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.Objects;

/**
 * Endpoint wrapper for the read-only category tax-settings resource. Reached
 * through {@code AllegroClient.settings().taxSettings(categoryId)} rather than a
 * dedicated sub-facade, since it is a single lookup.
 *
 * @since 0.3.0
 */
public final class TaxSettingsClientImpl {

    private static final String OP_TAX_SETTINGS = "get category tax settings";
    /** Spec query name — a dotted parameter, {@code category.id}. */
    private static final String PARAM_CATEGORY_ID = "category.id";
    private static final String ERR_CATEGORY_ID_NULL = "categoryId must not be null";

    private final HttpSupport http;

    public TaxSettingsClientImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /** Read the tax options available for the given category. */
    public TaxSettings taxSettings(String categoryId) {
        Objects.requireNonNull(categoryId, ERR_CATEGORY_ID_NULL);
        return TaxSettings.from(http.request(OP_TAX_SETTINGS)
                .get(ApiPaths.TAX_SETTINGS)
                .query(Query.create().add(PARAM_CATEGORY_ID, categoryId))
                .fetch(CategoryTaxSettingsRaw.class));
    }
}
