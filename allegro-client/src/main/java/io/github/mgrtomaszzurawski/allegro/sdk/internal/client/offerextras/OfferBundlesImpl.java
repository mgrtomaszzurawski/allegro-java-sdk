/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferBundleDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBundlesDTORaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferBundle;
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
 * Endpoint wrapper behind the {@link OfferBundles} facade.
 *
 * @since 0.2.0
 */
public final class OfferBundlesImpl implements OfferBundles {

    private static final String OP_STREAM = "stream offer bundles";
    private static final String OP_GET = "get offer bundle";
    private static final String OP_UPDATE_DISCOUNT = "update offer bundle discount";
    private static final String OP_DELETE = "delete offer bundle";

    /** Bundle list page size; the endpoint is cursor-paged by {@code page.id}. */
    private static final int PAGE_SIZE = 100;

    private static final String QUERY_PAGE_ID = "page.id";
    private static final String QUERY_LIMIT = "limit";

    private static final String ERR_BUNDLE_ID_NULL = "bundleId must not be null";
    private static final String ERR_DISCOUNTS_NULL = "discounts must not be null";

    private final HttpSupport http;

    public OfferBundlesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<OfferBundle> streamBundles() {
        return PagedSpliterator.cursorStream(this::fetchPage);
    }

    private PagedSpliterator.CursorPage<OfferBundle> fetchPage(@Nullable String cursor) {
        OfferBundlesDTORaw raw = http.request(OP_STREAM)
                .get(ApiPaths.BUNDLES)
                .query(Query.create().add(QUERY_PAGE_ID, cursor).add(QUERY_LIMIT, PAGE_SIZE))
                .fetch(OfferBundlesDTORaw.class);
        List<OfferBundle> items = OfferBundle.listFrom(raw);
        String nextCursor = raw.getNextPage() == null ? null : raw.getNextPage().getId();
        return new PagedSpliterator.CursorPage<>(items, nextCursor);
    }

    @Override
    public OfferBundle get(String bundleId) {
        Objects.requireNonNull(bundleId, ERR_BUNDLE_ID_NULL);
        return OfferBundle.from(http.request(OP_GET)
                .get(ApiPaths.bundle(bundleId))
                .fetch(OfferBundleDTORaw.class));
    }

    @Override
    public OfferBundle updateDiscount(String bundleId, List<BundleDiscount> discounts) {
        Objects.requireNonNull(bundleId, ERR_BUNDLE_ID_NULL);
        Objects.requireNonNull(discounts, ERR_DISCOUNTS_NULL);
        return OfferBundle.from(http.request(OP_UPDATE_DISCOUNT)
                .put(ApiPaths.bundleDiscount(bundleId))
                .jsonBody(OfferExtrasMapper.toDiscountsRaw(discounts))
                .fetch(OfferBundleDTORaw.class));
    }

    @Override
    public void delete(String bundleId) {
        Objects.requireNonNull(bundleId, ERR_BUNDLE_ID_NULL);
        http.request(OP_DELETE)
                .delete(ApiPaths.bundle(bundleId))
                .send();
    }
}
