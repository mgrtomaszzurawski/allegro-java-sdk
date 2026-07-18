/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The query passed to {@code allegroPrices().streamOffersStatus(...)}. The
 * {@code marketplaceId} is required (Allegro Prices status is per marketplace);
 * the offer ids, {@link OfferScope} and {@link OfferSubstatus} are optional
 * filters. Pagination is handled internally by the lazy stream.
 *
 * <pre>{@code
 * AllegroPricesOfferQuery query = AllegroPricesOfferQuery.builder("allegro-pl")
 *         .scope(OfferScope.DISCOUNTED)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class AllegroPricesOfferQuery {

    private final String marketplaceId;
    private final List<String> offerIds;
    private final @Nullable OfferScope scope;
    private final @Nullable OfferSubstatus substatus;

    private AllegroPricesOfferQuery(Builder builder) {
        this.marketplaceId = builder.marketplaceId;
        this.offerIds = List.copyOf(builder.offerIds);
        this.scope = builder.scope;
        this.substatus = builder.substatus;
    }

    /** The marketplace the status query is scoped to. */
    public String marketplaceId() {
        return marketplaceId;
    }

    /** The offer ids to restrict the query to; never {@code null}, possibly empty (= all offers). */
    public List<String> offerIds() {
        return offerIds;
    }

    /** The scope filter, or {@code null} for no scope filter. */
    public @Nullable OfferScope scope() {
        return scope;
    }

    /** The sub-status filter, or {@code null} for no sub-status filter. */
    public @Nullable OfferSubstatus substatus() {
        return substatus;
    }

    /**
     * A new builder for the given marketplace (required).
     *
     * @param marketplaceId the marketplace (e.g. {@code "allegro-pl"}); must not be null or blank
     * @return a new builder
     */
    public static Builder builder(String marketplaceId) {
        return new Builder(marketplaceId);
    }

    /** A builder pre-filled from this query. */
    public Builder toBuilder() {
        Builder builder = new Builder(marketplaceId).scope(scope).substatus(substatus);
        offerIds.forEach(builder::addOfferId);
        return builder;
    }

    /** Fluent builder for {@link AllegroPricesOfferQuery}; validates the marketplace fail-fast. */
    public static final class Builder {

        private static final String ERR_MARKETPLACE_REQUIRED = "marketplaceId is required";

        private final String marketplaceId;
        private final List<String> offerIds = new ArrayList<>();
        private @Nullable OfferScope scope;
        private @Nullable OfferSubstatus substatus;

        private Builder(String marketplaceId) {
            if (marketplaceId == null || marketplaceId.isBlank()) {
                throw new IllegalArgumentException(ERR_MARKETPLACE_REQUIRED);
            }
            this.marketplaceId = marketplaceId;
        }

        /** Restrict the query to an additional offer id. */
        public Builder addOfferId(String offerId) {
            offerIds.add(offerId);
            return this;
        }

        /** Filter by Allegro Prices scope. */
        public Builder scope(@Nullable OfferScope offerScope) {
            this.scope = offerScope;
            return this;
        }

        /** Filter by Allegro Prices sub-status. */
        public Builder substatus(@Nullable OfferSubstatus offerSubstatus) {
            this.substatus = offerSubstatus;
            return this;
        }

        /** Build the immutable query. */
        public AllegroPricesOfferQuery build() {
            return new AllegroPricesOfferQuery(this);
        }
    }
}
