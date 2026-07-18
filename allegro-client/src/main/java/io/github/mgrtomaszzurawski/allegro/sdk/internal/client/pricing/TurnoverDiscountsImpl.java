/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.client.model.NullableTurnoverDiscountDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountThresholdDtoDiscountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountThresholdDtoMinimumTurnoverRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountThresholdDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.TurnoverDiscounts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverThreshold;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint wrapper behind the {@link TurnoverDiscounts} facade. The list
 * endpoint returns a bare JSON array whose elements may be {@code null}, so null
 * elements are filtered out before mapping.
 *
 * @since 0.3.0
 */
public final class TurnoverDiscountsImpl implements TurnoverDiscounts {

    private static final String OP_LIST = "list turnover discounts";
    private static final String OP_SET = "set turnover discount";
    private static final String OP_DEACTIVATE = "deactivate turnover discount";
    private static final String QUERY_MARKETPLACE_ID = "marketplaceId";

    private final HttpSupport http;

    public TurnoverDiscountsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<TurnoverDiscount> list() {
        return listWithQuery(Query.create());
    }

    @Override
    public List<TurnoverDiscount> list(String marketplaceId) {
        return listWithQuery(Query.create().add(QUERY_MARKETPLACE_ID, marketplaceId));
    }

    private List<TurnoverDiscount> listWithQuery(Query query) {
        NullableTurnoverDiscountDtoRaw[] response = http.request(OP_LIST)
                .get(ApiPaths.TURNOVER_DISCOUNT)
                .query(query)
                .fetch(NullableTurnoverDiscountDtoRaw[].class);
        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(TurnoverDiscount::fromNullable)
                .toList();
    }

    @Override
    public TurnoverDiscount set(String marketplaceId, TurnoverDiscountRequest request) {
        TurnoverDiscountDtoRaw response = http.request(OP_SET)
                .put(ApiPaths.subPath(ApiPaths.TURNOVER_DISCOUNT, marketplaceId))
                .jsonBody(toRaw(request))
                .fetch(TurnoverDiscountDtoRaw.class);
        return TurnoverDiscount.from(response);
    }

    @Override
    public TurnoverDiscount deactivate(String marketplaceId) {
        TurnoverDiscountDtoRaw response = http.request(OP_DEACTIVATE)
                .put(ApiPaths.turnoverDiscountDeactivate(marketplaceId))
                .fetch(TurnoverDiscountDtoRaw.class);
        return TurnoverDiscount.from(response);
    }

    private static TurnoverDiscountRequestRaw toRaw(TurnoverDiscountRequest request) {
        return new TurnoverDiscountRequestRaw()
                .thresholds(request.thresholds().stream()
                        .map(TurnoverDiscountsImpl::thresholdToRaw)
                        .toList());
    }

    private static TurnoverDiscountThresholdDtoRaw thresholdToRaw(TurnoverThreshold threshold) {
        return new TurnoverDiscountThresholdDtoRaw()
                .minimumTurnover(new TurnoverDiscountThresholdDtoMinimumTurnoverRaw()
                        .amount(threshold.minimumTurnover().amount())
                        .currency(threshold.minimumTurnover().currency()))
                .discount(new TurnoverDiscountThresholdDtoDiscountRaw()
                        .percentage(threshold.discountPercentage()));
    }
}
