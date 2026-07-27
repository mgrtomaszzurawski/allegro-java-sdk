/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.additionalservices;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceGroupTranslationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoriesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.AdditionalServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.GroupTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link AdditionalServices} facade (read surface).
 *
 * @since 0.3.0
 */
public final class AdditionalServicesImpl implements AdditionalServices {

    private static final String OP_CATEGORY_DEFINITIONS = "list additional-service definitions";
    private static final String OP_STREAM_GROUPS = "list additional-services groups";
    private static final String OP_GET_GROUP = "get additional-services group";
    private static final String OP_GET_TRANSLATIONS = "get additional-services group translations";

    private static final String TRANSLATIONS_SEGMENT = "translations";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";
    /** Spec cap on {@code limit} for the groups list (also its default). */
    private static final int PAGE_LIMIT = 100;

    private static final String ERR_GROUP_ID_NULL = "groupId must not be null";

    private final HttpSupport http;

    public AdditionalServicesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<AdditionalServiceCategory> categoryDefinitions() {
        CategoriesResponseRaw raw = http.request(OP_CATEGORY_DEFINITIONS)
                .get(ApiPaths.ADDITIONAL_SERVICES_CATEGORIES)
                .fetch(CategoriesResponseRaw.class);
        List<CategoryResponseRaw> categories = raw.getCategories() == null ? List.of() : raw.getCategories();
        return categories.stream().map(AdditionalServiceCategory::from).toList();
    }

    @Override
    public Stream<AdditionalServicesGroup> streamGroups() {
        return PagedSpliterator.stream(this::fetchGroupPage);
    }

    private PagedSpliterator.Page<AdditionalServicesGroup> fetchGroupPage(int pageIndex) {
        int offset = pageIndex * PAGE_LIMIT;
        AdditionalServicesGroupsRaw page = http.request(OP_STREAM_GROUPS)
                .get(ApiPaths.ADDITIONAL_SERVICES_GROUPS)
                .query(Query.create().add(PARAM_OFFSET, offset).add(PARAM_LIMIT, PAGE_LIMIT))
                .fetch(AdditionalServicesGroupsRaw.class);
        List<AdditionalServicesGroupResponseRaw> items =
                page.getAdditionalServicesGroups() == null ? List.of() : page.getAdditionalServicesGroups();
        List<AdditionalServicesGroup> groups = items.stream().map(AdditionalServicesGroup::from).toList();
        return new PagedSpliterator.Page<>(groups, groups.size() == PAGE_LIMIT);
    }

    @Override
    public AdditionalServicesGroup group(String groupId) {
        Objects.requireNonNull(groupId, ERR_GROUP_ID_NULL);
        return AdditionalServicesGroup.from(http.request(OP_GET_GROUP)
                .get(ApiPaths.subPath(ApiPaths.ADDITIONAL_SERVICES_GROUPS, groupId))
                .fetch(AdditionalServicesGroupResponseRaw.class));
    }

    @Override
    public GroupTranslations translations(String groupId) {
        Objects.requireNonNull(groupId, ERR_GROUP_ID_NULL);
        return GroupTranslations.from(http.request(OP_GET_TRANSLATIONS)
                .get(ApiPaths.subPath(ApiPaths.ADDITIONAL_SERVICES_GROUPS, groupId, TRANSLATIONS_SEGMENT))
                .fetch(AdditionalServiceGroupTranslationResponseRaw.class));
    }
}
