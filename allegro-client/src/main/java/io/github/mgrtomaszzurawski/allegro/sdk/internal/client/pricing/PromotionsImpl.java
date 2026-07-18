/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.client.model.SellerRebateDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerRebatesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Promotions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionType;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Promotions} facade
 * ({@code /sale/loyalty/promotions}). The list endpoint is genuinely paginated
 * (offset/limit with {@code totalCount}), so it is exposed as a lazy
 * {@link Stream}. Reads and writes map through {@link PromotionsMapper}; the
 * polymorphic {@code benefits[]} deserialize natively — an unknown benefit type
 * degrades to the base and is surfaced as a sentinel by the mapper.
 *
 * @since 0.4.0
 */
public final class PromotionsImpl implements Promotions {

    private static final int PAGE_SIZE = 100;

    private static final String OP_STREAM = "stream promotions";
    private static final String OP_GET = "get promotion";
    private static final String OP_CREATE = "create promotion";
    private static final String OP_MODIFY = "modify promotion";
    private static final String OP_DEACTIVATE = "deactivate promotion";

    private static final String QUERY_PROMOTION_TYPE = "promotionType";
    private static final String QUERY_OFFER_ID = "offer.id";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private static final String ERR_NULL_TYPE = "type must not be null";
    private static final String ERR_NULL_REQUEST = "request must not be null";
    private static final String ERR_NULL_PROMOTION_ID = "promotionId must not be null";

    private final HttpSupport http;

    public PromotionsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<Promotion> streamPromotions(PromotionType type) {
        return stream(type, null);
    }

    @Override
    public Stream<Promotion> streamPromotions(PromotionType type, String offerId) {
        return stream(type, offerId);
    }

    private Stream<Promotion> stream(PromotionType type, @Nullable String offerId) {
        if (type == null) {
            throw new IllegalArgumentException(ERR_NULL_TYPE);
        }
        return PagedSpliterator.stream(pageIndex -> fetchPage(type, offerId, pageIndex));
    }

    private PagedSpliterator.Page<Promotion> fetchPage(
            PromotionType type, @Nullable String offerId, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_PROMOTION_TYPE, type.name())
                .add(QUERY_OFFER_ID, offerId)
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        SellerRebatesDtoRaw response = http.request(OP_STREAM)
                .get(ApiPaths.LOYALTY_PROMOTIONS)
                .query(query)
                .fetch(SellerRebatesDtoRaw.class);
        List<Promotion> items = response.getPromotions().stream()
                .map(PromotionsMapper::from)
                .toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    private static boolean hasMore(int offset, int pageCount, long totalCount) {
        return (long) offset + pageCount < totalCount;
    }

    @Override
    public Promotion get(String promotionId) {
        if (promotionId == null) {
            throw new IllegalArgumentException(ERR_NULL_PROMOTION_ID);
        }
        SellerRebateDtoRaw response = http.request(OP_GET)
                .get(ApiPaths.subPath(ApiPaths.LOYALTY_PROMOTIONS, promotionId))
                .fetch(SellerRebateDtoRaw.class);
        return PromotionsMapper.from(response);
    }

    @Override
    public Promotion create(PromotionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(ERR_NULL_REQUEST);
        }
        SellerRebateDtoRaw response = http.request(OP_CREATE)
                .post(ApiPaths.LOYALTY_PROMOTIONS)
                .jsonBody(PromotionsMapper.toRaw(request))
                .fetch(SellerRebateDtoRaw.class);
        return PromotionsMapper.from(response);
    }

    @Override
    public Promotion modify(String promotionId, PromotionRequest request) {
        if (promotionId == null) {
            throw new IllegalArgumentException(ERR_NULL_PROMOTION_ID);
        }
        if (request == null) {
            throw new IllegalArgumentException(ERR_NULL_REQUEST);
        }
        SellerRebateDtoRaw response = http.request(OP_MODIFY)
                .put(ApiPaths.subPath(ApiPaths.LOYALTY_PROMOTIONS, promotionId))
                .jsonBody(PromotionsMapper.toRaw(request))
                .fetch(SellerRebateDtoRaw.class);
        return PromotionsMapper.from(response);
    }

    @Override
    public void deactivate(String promotionId) {
        if (promotionId == null) {
            throw new IllegalArgumentException(ERR_NULL_PROMOTION_ID);
        }
        http.request(OP_DEACTIVATE)
                .delete(ApiPaths.subPath(ApiPaths.LOYALTY_PROMOTIONS, promotionId))
                .send();
    }
}
