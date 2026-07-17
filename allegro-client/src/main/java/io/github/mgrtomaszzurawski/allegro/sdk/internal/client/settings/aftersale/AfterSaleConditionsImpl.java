/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.client.model.WarrantiesListWarrantyBasicRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyBasicRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link AfterSaleConditions} facade.
 *
 * @since 0.2.0
 */
public final class AfterSaleConditionsImpl implements AfterSaleConditions {

    private static final String OP_STREAM_WARRANTIES = "list warranties";
    private static final String OP_GET_WARRANTY = "get warranty";
    private static final String OP_CREATE_WARRANTY = "create warranty";
    private static final String OP_UPDATE_WARRANTY = "update warranty";

    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";
    /** Page size when streaming; the max the endpoint accepts is 100. */
    private static final int PAGE_LIMIT = 100;

    private static final String ERR_WARRANTY_ID_NULL = "warrantyId must not be null";
    private static final String ERR_REQUEST_NULL = "request must not be null";

    private final HttpSupport http;

    public AfterSaleConditionsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<WarrantySummary> streamWarranties() {
        return PagedSpliterator.stream(this::fetchWarrantyPage);
    }

    private PagedSpliterator.Page<WarrantySummary> fetchWarrantyPage(int pageIndex) {
        WarrantiesListWarrantyBasicRaw page = http.request(OP_STREAM_WARRANTIES)
                .get(ApiPaths.AFTER_SALES_WARRANTIES)
                .query(Query.create()
                        .add(PARAM_OFFSET, pageIndex * PAGE_LIMIT)
                        .add(PARAM_LIMIT, PAGE_LIMIT))
                .fetch(WarrantiesListWarrantyBasicRaw.class);
        List<WarrantyBasicRaw> items = page.getWarranties() == null ? List.of() : page.getWarranties();
        List<WarrantySummary> summaries = items.stream().map(WarrantySummary::from).toList();
        // The list response carries no totalCount, so "there is another page"
        // is inferred from a full page — a short/empty page ends the walk.
        boolean hasMore = summaries.size() == PAGE_LIMIT;
        return new PagedSpliterator.Page<>(summaries, hasMore);
    }

    @Override
    public Warranty warranty(String warrantyId) {
        Objects.requireNonNull(warrantyId, ERR_WARRANTY_ID_NULL);
        return Warranty.from(http.request(OP_GET_WARRANTY)
                .get(ApiPaths.subPath(ApiPaths.AFTER_SALES_WARRANTIES, warrantyId))
                .fetch(WarrantyResponseRaw.class));
    }

    @Override
    public Warranty createWarranty(WarrantyRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return Warranty.from(http.request(OP_CREATE_WARRANTY)
                .post(ApiPaths.AFTER_SALES_WARRANTIES)
                .jsonBody(WarrantyMapper.toRaw(request))
                .fetch(WarrantyResponseRaw.class));
    }

    @Override
    public Warranty updateWarranty(String warrantyId, WarrantyRequest request) {
        Objects.requireNonNull(warrantyId, ERR_WARRANTY_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return Warranty.from(http.request(OP_UPDATE_WARRANTY)
                .put(ApiPaths.subPath(ApiPaths.AFTER_SALES_WARRANTIES, warrantyId))
                .jsonBody(WarrantyMapper.toRaw(request))
                .fetch(WarrantyResponseRaw.class));
    }
}
