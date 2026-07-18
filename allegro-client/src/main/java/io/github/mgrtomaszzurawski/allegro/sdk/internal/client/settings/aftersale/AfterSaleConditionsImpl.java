/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantiesListImpliedWarrantyBasicRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyBasicRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPoliciesListReturnPolicyV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantiesListWarrantyBasicRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyBasicRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyUpdateRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicy;
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
    private static final String OP_STREAM_IMPLIED = "list implied warranties";
    private static final String OP_GET_IMPLIED = "get implied warranty";
    private static final String OP_CREATE_IMPLIED = "create implied warranty";
    private static final String OP_UPDATE_IMPLIED = "update implied warranty";
    private static final String OP_STREAM_RETURN_POLICIES = "list return policies";
    private static final String OP_GET_RETURN_POLICY = "get return policy";
    private static final String OP_CREATE_RETURN_POLICY = "create return policy";
    private static final String OP_UPDATE_RETURN_POLICY = "update return policy";
    private static final String OP_DELETE_RETURN_POLICY = "delete return policy";

    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";
    /** Spec cap on {@code limit} for these endpoints (also their default). */
    private static final int PAGE_LIMIT = 60;
    /** Spec cap on {@code offset}: the endpoints serve a single page, not deep pagination. */
    private static final int MAX_OFFSET = 59;

    private static final String ERR_WARRANTY_ID_NULL = "warrantyId must not be null";
    private static final String ERR_IMPLIED_ID_NULL = "impliedWarrantyId must not be null";
    private static final String ERR_RETURN_POLICY_ID_NULL = "returnPolicyId must not be null";
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
        int offset = pageIndex * PAGE_LIMIT;
        WarrantiesListWarrantyBasicRaw page = http.request(OP_STREAM_WARRANTIES)
                .get(ApiPaths.AFTER_SALES_WARRANTIES)
                .query(pageQuery(offset))
                .fetch(WarrantiesListWarrantyBasicRaw.class);
        List<WarrantyBasicRaw> items = page.getWarranties() == null ? List.of() : page.getWarranties();
        List<WarrantySummary> summaries = items.stream().map(WarrantySummary::from).toList();
        return new PagedSpliterator.Page<>(summaries, hasNextPage(summaries.size(), offset));
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

    @Override
    public Stream<ImpliedWarrantySummary> streamImpliedWarranties() {
        return PagedSpliterator.stream(this::fetchImpliedWarrantyPage);
    }

    private PagedSpliterator.Page<ImpliedWarrantySummary> fetchImpliedWarrantyPage(int pageIndex) {
        int offset = pageIndex * PAGE_LIMIT;
        ImpliedWarrantiesListImpliedWarrantyBasicRaw page = http.request(OP_STREAM_IMPLIED)
                .get(ApiPaths.AFTER_SALES_IMPLIED_WARRANTIES)
                .query(pageQuery(offset))
                .fetch(ImpliedWarrantiesListImpliedWarrantyBasicRaw.class);
        List<ImpliedWarrantyBasicRaw> items =
                page.getImpliedWarranties() == null ? List.of() : page.getImpliedWarranties();
        List<ImpliedWarrantySummary> summaries =
                items.stream().map(ImpliedWarrantySummary::from).toList();
        return new PagedSpliterator.Page<>(summaries, hasNextPage(summaries.size(), offset));
    }

    @Override
    public ImpliedWarranty impliedWarranty(String impliedWarrantyId) {
        Objects.requireNonNull(impliedWarrantyId, ERR_IMPLIED_ID_NULL);
        return ImpliedWarranty.from(http.request(OP_GET_IMPLIED)
                .get(ApiPaths.subPath(ApiPaths.AFTER_SALES_IMPLIED_WARRANTIES, impliedWarrantyId))
                .fetch(ImpliedWarrantyResponseRaw.class));
    }

    @Override
    public ImpliedWarranty createImpliedWarranty(ImpliedWarrantyRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return ImpliedWarranty.from(http.request(OP_CREATE_IMPLIED)
                .post(ApiPaths.AFTER_SALES_IMPLIED_WARRANTIES)
                .jsonBody(ImpliedWarrantyMapper.toRaw(request))
                .fetch(ImpliedWarrantyResponseRaw.class));
    }

    @Override
    public ImpliedWarranty updateImpliedWarranty(String impliedWarrantyId, ImpliedWarrantyRequest request) {
        Objects.requireNonNull(impliedWarrantyId, ERR_IMPLIED_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return ImpliedWarranty.from(http.request(OP_UPDATE_IMPLIED)
                .put(ApiPaths.subPath(ApiPaths.AFTER_SALES_IMPLIED_WARRANTIES, impliedWarrantyId))
                .jsonBody(ImpliedWarrantyMapper.toRaw(request))
                .fetch(ImpliedWarrantyResponseRaw.class));
    }

    @Override
    public Stream<ReturnPolicy> streamReturnPolicies() {
        return PagedSpliterator.stream(this::fetchReturnPolicyPage);
    }

    private PagedSpliterator.Page<ReturnPolicy> fetchReturnPolicyPage(int pageIndex) {
        int offset = pageIndex * PAGE_LIMIT;
        ReturnPoliciesListReturnPolicyV1Raw page = http.request(OP_STREAM_RETURN_POLICIES)
                .get(ApiPaths.AFTER_SALES_RETURN_POLICIES)
                .query(pageQuery(offset))
                .fetch(ReturnPoliciesListReturnPolicyV1Raw.class);
        List<ReturnPolicyResponseV1Raw> items =
                page.getReturnPolicies() == null ? List.of() : page.getReturnPolicies();
        List<ReturnPolicy> policies = items.stream().map(ReturnPolicy::from).toList();
        return new PagedSpliterator.Page<>(policies, hasNextPage(policies.size(), offset));
    }

    @Override
    public ReturnPolicy returnPolicy(String returnPolicyId) {
        Objects.requireNonNull(returnPolicyId, ERR_RETURN_POLICY_ID_NULL);
        return ReturnPolicy.from(http.request(OP_GET_RETURN_POLICY)
                .get(ApiPaths.subPath(ApiPaths.AFTER_SALES_RETURN_POLICIES, returnPolicyId))
                .fetch(ReturnPolicyResponseV1Raw.class));
    }

    @Override
    public ReturnPolicy createReturnPolicy(ReturnPolicyRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return ReturnPolicy.from(http.request(OP_CREATE_RETURN_POLICY)
                .post(ApiPaths.AFTER_SALES_RETURN_POLICIES)
                .jsonBody(ReturnPolicyMapper.toRaw(request))
                .fetch(ReturnPolicyResponseV1Raw.class));
    }

    @Override
    public ReturnPolicy updateReturnPolicy(String returnPolicyId, ReturnPolicyUpdateRequest request) {
        Objects.requireNonNull(returnPolicyId, ERR_RETURN_POLICY_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return ReturnPolicy.from(http.request(OP_UPDATE_RETURN_POLICY)
                .put(ApiPaths.subPath(ApiPaths.AFTER_SALES_RETURN_POLICIES, returnPolicyId))
                .jsonBody(ReturnPolicyMapper.toUpdateRaw(request))
                .fetch(ReturnPolicyResponseV1Raw.class));
    }

    @Override
    public void deleteReturnPolicy(String returnPolicyId) {
        Objects.requireNonNull(returnPolicyId, ERR_RETURN_POLICY_ID_NULL);
        // The server returns the deleted policy; the SDK discards that body.
        http.request(OP_DELETE_RETURN_POLICY)
                .delete(ApiPaths.subPath(ApiPaths.AFTER_SALES_RETURN_POLICIES, returnPolicyId))
                .send();
    }

    private static Query pageQuery(int offset) {
        return Query.create()
                .add(PARAM_OFFSET, offset)
                .add(PARAM_LIMIT, PAGE_LIMIT);
    }

    /**
     * Whether the lazy walk should request another page. These endpoints cap
     * {@code offset} at {@link #MAX_OFFSET} (59), below one full page
     * ({@link #PAGE_LIMIT} = 60), so a page-aligned next offset is always out of
     * range and the server would reject it. The full-page check is therefore
     * defensive, never decisive — the walk always stops after the first page;
     * the guard still holds should the caps ever widen.
     */
    private static boolean hasNextPage(int pageSize, int offset) {
        return pageSize == PAGE_LIMIT && offset + PAGE_LIMIT <= MAX_OFFSET;
    }
}
