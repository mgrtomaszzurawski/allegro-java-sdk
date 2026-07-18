/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundRejectionRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundRejectionRequestRejectionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.CustomerReturns;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RejectionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ReturnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.CustomerReturn;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrappers behind the {@link CustomerReturns} sub-facade (BETA media
 * type on every call).
 *
 * @since 0.6.0
 */
public final class CustomerReturnsImpl implements CustomerReturns {

    private static final int PAGE_SIZE = 100;

    private static final String OP_STREAM = "stream customer returns";
    private static final String OP_GET = "get customer return";
    private static final String OP_REJECT = "reject customer return refund";

    private static final String QUERY_ORDER_ID = "orderId";
    private static final String QUERY_BUYER_LOGIN = "buyer.login";
    private static final String QUERY_BUYER_EMAIL = "buyer.email";
    private static final String QUERY_REFERENCE_NUMBER = "referenceNumber";
    private static final String QUERY_CREATED_GTE = "createdAt.gte";
    private static final String QUERY_CREATED_LTE = "createdAt.lte";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public CustomerReturnsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<CustomerReturn> streamReturns(ReturnFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<CustomerReturn> fetchPage(ReturnFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_ORDER_ID, filter.orderId())
                .add(QUERY_BUYER_LOGIN, filter.buyerLogin())
                .add(QUERY_BUYER_EMAIL, filter.buyerEmail())
                .add(QUERY_REFERENCE_NUMBER, filter.referenceNumber())
                .add(QUERY_CREATED_GTE, filter.createdFrom())
                .add(QUERY_CREATED_LTE, filter.createdTo())
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        CustomerReturnResponseRaw response = http.request(OP_STREAM)
                .get(ApiPaths.CUSTOMER_RETURNS)
                .acceptBeta()
                .query(query)
                .fetch(CustomerReturnResponseRaw.class);
        List<CustomerReturnRaw> returns = response.getCustomerReturns();
        List<CustomerReturn> items = returns == null
                ? List.of()
                : returns.stream().map(CustomerReturn::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getCount()));
    }

    private static boolean hasMore(int offset, int pageCount, @Nullable Long total) {
        if (total == null) {
            return pageCount == PAGE_SIZE;
        }
        return (long) offset + pageCount < total;
    }

    @Override
    public CustomerReturn get(String customerReturnId) {
        return CustomerReturn.from(http.request(OP_GET)
                .get(ApiPaths.subPath(ApiPaths.CUSTOMER_RETURNS, customerReturnId))
                .acceptBeta()
                .fetch(CustomerReturnRaw.class));
    }

    @Override
    public CustomerReturn rejectRefund(String customerReturnId, RejectionRequest request) {
        CustomerReturnRefundRejectionRequestRaw body = new CustomerReturnRefundRejectionRequestRaw()
                .rejection(new CustomerReturnRefundRejectionRequestRejectionRaw()
                        .code(request.code().toRaw())
                        .reason(request.reason()));
        // KNOWN LIMITATION: this beta POST body still goes out with the v1 vendor
        // Content-Type — acceptBeta() only sets Accept, and HttpCall.jsonBody hard-pins
        // Content-Type=v1 (frozen runtime). The beta endpoint may reject that; a
        // beta JSON-body variant is a filed core need (see KNOWN-SERVER-BEHAVIORS.md).
        return CustomerReturn.from(http.request(OP_REJECT)
                .post(ApiPaths.subPath(ApiPaths.CUSTOMER_RETURNS, customerReturnId,
                        ApiPaths.REJECTION_SEGMENT))
                .acceptBeta()
                .jsonBody(body)
                .fetch(CustomerReturnRaw.class));
    }
}
