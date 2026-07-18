/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders;

import io.github.mgrtomaszzurawski.allegro.client.model.GetRefundApplications200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimRequestLineItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundClaimResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.CommissionRefunds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ClaimFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RefundClaimRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.RefundClaim;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint wrappers behind the {@link CommissionRefunds} sub-facade.
 *
 * <p>The refund-claims list carries no {@code totalCount}, so pagination
 * terminates when a page comes back shorter than the requested page size.
 *
 * @since 0.6.0
 */
public final class CommissionRefundsImpl implements CommissionRefunds {

    private static final int PAGE_SIZE = 100;

    private static final String OP_STREAM = "stream refund claims";
    private static final String OP_GET = "get refund claim";
    private static final String OP_CLAIM = "create refund claim";
    private static final String OP_CANCEL = "cancel refund claim";

    private static final String QUERY_OFFER_ID = "lineItem.offer.id";
    private static final String QUERY_BUYER_ID = "buyer.id";
    private static final String QUERY_STATUS = "status";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public CommissionRefundsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<RefundClaim> streamClaims(ClaimFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<RefundClaim> fetchPage(ClaimFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(QUERY_OFFER_ID, filter.offerId())
                .add(QUERY_BUYER_ID, filter.buyerId())
                .add(QUERY_STATUS, filter.status())
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        GetRefundApplications200ResponseRaw response = http.request(OP_STREAM)
                .get(ApiPaths.REFUND_CLAIMS)
                .query(query)
                .fetch(GetRefundApplications200ResponseRaw.class);
        List<RefundClaimRaw> claims = response.getRefundClaims();
        List<RefundClaim> items = claims == null
                ? List.of()
                : claims.stream().map(RefundClaim::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public RefundClaim get(String claimId) {
        return RefundClaim.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.REFUND_CLAIMS, claimId), RefundClaimRaw.class, OP_GET));
    }

    @Override
    public String claim(RefundClaimRequest request) {
        RefundClaimRequestRaw body = new RefundClaimRequestRaw()
                .lineItem(new RefundClaimRequestLineItemRaw().id(request.lineItemId()))
                .quantity(request.quantity());
        RefundClaimResponseRaw created = http.postJsonAuthenticated(
                ApiPaths.REFUND_CLAIMS, body, RefundClaimResponseRaw.class, OP_CLAIM);
        return created.getId();
    }

    @Override
    public void cancel(String claimId) {
        http.deleteAuthenticated(ApiPaths.subPath(ApiPaths.REFUND_CLAIMS, claimId), OP_CANCEL);
    }
}
