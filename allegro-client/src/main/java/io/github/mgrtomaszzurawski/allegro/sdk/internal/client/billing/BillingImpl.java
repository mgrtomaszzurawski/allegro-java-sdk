/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.billing;

import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntriesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingEntryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BillingTypeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.Billing;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder.BillingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingType;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint wrappers behind the {@link Billing} facade.
 *
 * <p>The billing-entries response carries no {@code totalCount}, so pagination
 * terminates when a page comes back shorter than the requested page size.
 *
 * @since 0.5.0
 */
public final class BillingImpl implements Billing {

    private static final int PAGE_SIZE = 100;

    private static final String OP_STREAM_ENTRIES = "stream billing entries";
    private static final String OP_TYPES = "list billing types";

    private static final String QUERY_MARKETPLACE_ID = "marketplaceId";
    private static final String QUERY_OCCURRED_GTE = "occurredAt.gte";
    private static final String QUERY_OCCURRED_LTE = "occurredAt.lte";
    private static final String QUERY_TYPE_ID = "type.id";
    private static final String QUERY_OFFER_ID = "offer.id";
    private static final String QUERY_ORDER_ID = "order.id";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public BillingImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<BillingEntry> streamEntries(BillingFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<BillingEntry> fetchPage(BillingFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(QUERY_MARKETPLACE_ID, filter.marketplaceId())
                .add(QUERY_OCCURRED_GTE, filter.occurredFrom())
                .add(QUERY_OCCURRED_LTE, filter.occurredTo())
                .add(QUERY_TYPE_ID, filter.typeId())
                .add(QUERY_OFFER_ID, filter.offerId())
                .add(QUERY_ORDER_ID, filter.orderId())
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        BillingEntriesRaw response = http.request(OP_STREAM_ENTRIES)
                .get(ApiPaths.BILLING_ENTRIES)
                .query(query)
                .fetch(BillingEntriesRaw.class);
        List<BillingEntryRaw> entries = response.getBillingEntries();
        List<BillingEntry> items = entries == null
                ? List.of()
                : entries.stream().map(BillingEntry::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public List<BillingType> types() {
        BillingTypeRaw[] types = http.getAuthenticated(
                ApiPaths.BILLING_TYPES, BillingTypeRaw[].class, OP_TYPES);
        return Arrays.stream(types).map(BillingType::from).toList();
    }
}
