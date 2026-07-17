/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.classifieds;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageConfigsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.Classifieds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint wrapper behind the {@link Classifieds} facade.
 *
 * @since 0.2.0
 */
public final class ClassifiedsImpl implements Classifieds {

    private static final String OP_AVAILABLE_PACKAGES = "list classifieds packages for category";
    private static final String PARAM_CATEGORY_ID = "category.id";
    private static final String ERR_CATEGORY_ID_NULL = "categoryId must not be null";

    private final HttpSupport http;

    public ClassifiedsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<ClassifiedPackage> availablePackages(String categoryId) {
        Objects.requireNonNull(categoryId, ERR_CATEGORY_ID_NULL);
        ClassifiedPackageConfigsRaw raw = http.request(OP_AVAILABLE_PACKAGES)
                .get(ApiPaths.CLASSIFIEDS_PACKAGES)
                .query(Query.create().add(PARAM_CATEGORY_ID, categoryId))
                .fetch(ClassifiedPackageConfigsRaw.class);
        return ClassifiedPackage.listFrom(raw);
    }
}
