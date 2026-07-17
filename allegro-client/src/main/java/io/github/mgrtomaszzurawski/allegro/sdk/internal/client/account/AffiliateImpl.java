/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.Affiliate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.ConversionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CpsConversion;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Affiliate} facade (beta media type).
 *
 * <p>The conversion list response carries no {@code totalCount}, so pagination
 * terminates when a page comes back shorter than the requested page size.
 *
 * @since 0.2.0
 */
public final class AffiliateImpl implements Affiliate {

    private static final int PAGE_SIZE = 1000;
    private static final String KEY_SEPARATOR = ",";

    private static final String OP_STREAM = "stream cps conversions";
    private static final String QUERY_ORDER_GTE = "orderCreatedAt.gte";
    private static final String QUERY_ORDER_LTE = "orderCreatedAt.lte";
    private static final String QUERY_MODIFIED_GTE = "lastModifiedAt.gte";
    private static final String QUERY_MODIFIED_LTE = "lastModifiedAt.lte";
    private static final String QUERY_STATUS = "status";
    private static final String QUERY_INCLUDE_PARAMS = "includePublisherUrlParameters";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public AffiliateImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<CpsConversion> streamCpsConversions(ConversionFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<CpsConversion> fetchPage(ConversionFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(QUERY_ORDER_GTE, filter.orderCreatedFrom())
                .add(QUERY_ORDER_LTE, filter.orderCreatedTo())
                .add(QUERY_MODIFIED_GTE, filter.lastModifiedFrom())
                .add(QUERY_MODIFIED_LTE, filter.lastModifiedTo())
                .add(QUERY_STATUS, filter.status())
                .add(QUERY_INCLUDE_PARAMS, includeKeys(filter))
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        CpsConversionResponseRaw response = http.request(OP_STREAM)
                .get(ApiPaths.AFFILIATE_CPS_CONVERSIONS)
                .query(query)
                .acceptBeta()
                .fetch(CpsConversionResponseRaw.class);
        List<CpsConversionRaw> conversions = response.getConversions();
        List<CpsConversion> items = conversions == null
                ? List.of()
                : conversions.stream().map(CpsConversion::from).toList();
        boolean hasMore = items.size() == PAGE_SIZE;
        return new PagedSpliterator.Page<>(items, hasMore);
    }

    private static @Nullable String includeKeys(ConversionFilter filter) {
        List<String> keys = filter.includePublisherUrlParameterKeys();
        return keys.isEmpty() ? null : String.join(KEY_SEPARATOR, keys);
    }
}
