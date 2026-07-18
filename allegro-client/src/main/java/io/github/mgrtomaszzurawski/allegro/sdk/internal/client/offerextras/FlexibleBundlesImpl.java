/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleGetDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundlesListingDTORaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link FlexibleBundles} facade.
 *
 * @since 0.2.0
 */
public final class FlexibleBundlesImpl implements FlexibleBundles {

    private static final String OP_STREAM = "stream flexible bundles";
    private static final String OP_GET = "get flexible bundle";
    private static final String OP_DELETE = "delete flexible bundle";

    /** Flexible-bundle list page size; the endpoint is cursor-paged by {@code page.id}. */
    private static final int PAGE_SIZE = 100;

    private static final String QUERY_PAGE_ID = "page.id";
    private static final String QUERY_LIMIT = "limit";

    private static final String ERR_BUNDLE_ID_NULL = "bundleId must not be null";

    private final HttpSupport http;

    public FlexibleBundlesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<FlexibleBundleSummary> streamBundles() {
        return PagedSpliterator.cursorStream(this::fetchPage);
    }

    private PagedSpliterator.CursorPage<FlexibleBundleSummary> fetchPage(@Nullable String cursor) {
        FlexibleBundlesListingDTORaw raw = http.request(OP_STREAM)
                .get(ApiPaths.FLEXIBLE_BUNDLES)
                .query(Query.create().add(QUERY_PAGE_ID, cursor).add(QUERY_LIMIT, PAGE_SIZE))
                .fetch(FlexibleBundlesListingDTORaw.class);
        List<FlexibleBundleSummary> items = FlexibleBundleSummary.listFrom(raw);
        String nextCursor = raw.getNextPage() == null ? null : raw.getNextPage().getId();
        return new PagedSpliterator.CursorPage<>(items, nextCursor);
    }

    @Override
    public FlexibleBundle get(String bundleId) {
        Objects.requireNonNull(bundleId, ERR_BUNDLE_ID_NULL);
        return FlexibleBundle.from(http.request(OP_GET)
                .get(ApiPaths.flexibleBundle(bundleId))
                .fetch(FlexibleBundleGetDTORaw.class));
    }

    @Override
    public void delete(String bundleId) {
        Objects.requireNonNull(bundleId, ERR_BUNDLE_ID_NULL);
        http.request(OP_DELETE)
                .delete(ApiPaths.flexibleBundle(bundleId))
                .send();
    }
}
