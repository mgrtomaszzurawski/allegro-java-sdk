/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras;

import io.github.mgrtomaszzurawski.allegro.client.model.TagIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagListResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTags;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.Tag;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link OfferTags} facade.
 *
 * @since 0.2.0
 */
public final class OfferTagsImpl implements OfferTags {

    private static final String OP_STREAM = "stream offer tags";
    private static final String OP_CREATE = "create offer tag";
    private static final String OP_RENAME = "rename offer tag";
    private static final String OP_DELETE = "delete offer tag";
    private static final String OP_OF_OFFER = "get tags assigned to offer";
    private static final String OP_ASSIGN = "assign tags to offer";

    /** Tag list page ≤ 1000 (spec); 100 balances round-trips against payload size. */
    private static final int PAGE_SIZE = 100;

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private static final String ERR_REQUEST_NULL = "request must not be null";
    private static final String ERR_TAG_ID_NULL = "tagId must not be null";
    private static final String ERR_OFFER_ID_NULL = "offerId must not be null";
    private static final String ERR_TAG_IDS_NULL = "tagIds must not be null";

    private final HttpSupport http;

    public OfferTagsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<Tag> streamTags() {
        return PagedSpliterator.stream(this::fetchPage);
    }

    private PagedSpliterator.Page<Tag> fetchPage(int pageIndex) {
        TagListResponseRaw raw = http.request(OP_STREAM)
                .get(ApiPaths.OFFER_TAGS)
                .query(Query.create()
                        .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                        .add(QUERY_LIMIT, PAGE_SIZE))
                .fetch(TagListResponseRaw.class);
        List<Tag> items = Tag.listFrom(raw);
        // TagListResponse carries no totalCount, so a full page is the
        // "there may be more" signal shared with the other SDK streams.
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public String create(TagRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        TagIdRaw raw = http.request(OP_CREATE)
                .post(ApiPaths.OFFER_TAGS)
                .jsonBody(OfferExtrasMapper.toRaw(request))
                .fetch(TagIdRaw.class);
        return raw.getId();
    }

    @Override
    public void rename(String tagId, TagRequest request) {
        Objects.requireNonNull(tagId, ERR_TAG_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        http.request(OP_RENAME)
                .put(ApiPaths.offerTag(tagId))
                .jsonBody(OfferExtrasMapper.toRaw(request))
                .send();
    }

    @Override
    public void delete(String tagId) {
        Objects.requireNonNull(tagId, ERR_TAG_ID_NULL);
        http.request(OP_DELETE)
                .delete(ApiPaths.offerTag(tagId))
                .send();
    }

    @Override
    public List<Tag> ofOffer(String offerId) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        TagListResponseRaw raw = http.request(OP_OF_OFFER)
                .get(ApiPaths.offerAssignedTags(offerId))
                .fetch(TagListResponseRaw.class);
        return Tag.listFrom(raw);
    }

    @Override
    public void assignToOffer(String offerId, List<String> tagIds) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        Objects.requireNonNull(tagIds, ERR_TAG_IDS_NULL);
        http.request(OP_ASSIGN)
                .post(ApiPaths.offerAssignedTags(offerId))
                .jsonBody(OfferExtrasMapper.toIdsRaw(tagIds))
                .send();
    }
}
