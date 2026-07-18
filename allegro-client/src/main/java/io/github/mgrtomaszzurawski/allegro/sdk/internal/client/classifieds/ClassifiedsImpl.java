/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.classifieds;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageConfigRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageConfigsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatsResponseDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerOfferStatsResponseDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.Classifieds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder.ClassifiedStatsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifiedStats;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifieds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.SellerClassifiedStats;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint wrapper behind the {@link Classifieds} facade.
 *
 * @since 0.2.0
 */
public final class ClassifiedsImpl implements Classifieds {

    private static final String OP_AVAILABLE_PACKAGES = "list classifieds packages for category";
    private static final String OP_GET_PACKAGE = "get classifieds package configuration";
    private static final String OP_PACKAGES_OF_OFFER = "get classifieds packages of offer";
    private static final String OP_ASSIGN_PACKAGES = "assign classifieds packages to offer";
    private static final String OP_OFFER_STATS = "get classifieds offer statistics";
    private static final String OP_SELLER_STATS = "get classifieds seller statistics";
    private static final String PARAM_CATEGORY_ID = "category.id";
    private static final String PARAM_OFFER_ID = "offer.id";
    private static final String PARAM_DATE_GTE = "date.gte";
    private static final String PARAM_DATE_LTE = "date.lte";
    private static final String ERR_CATEGORY_ID_NULL = "categoryId must not be null";
    private static final String ERR_PACKAGE_ID_NULL = "packageId must not be null";
    private static final String ERR_OFFER_ID_NULL = "offerId must not be null";
    private static final String ERR_ASSIGNMENT_NULL = "assignment must not be null";
    private static final String ERR_OFFER_IDS_NULL = "offerIds must not be null";
    private static final String ERR_FILTER_NULL = "filter must not be null";
    private static final int MAX_OFFER_IDS = 50;
    private static final String ERR_OFFER_IDS_RANGE =
            "offerIds must contain between 1 and " + MAX_OFFER_IDS + " ids";

    private final HttpSupport http;

    public ClassifiedsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<ClassifiedPackage> availablePackages(String categoryId) {
        Objects.requireNonNull(categoryId, ERR_CATEGORY_ID_NULL);
        ClassifiedPackageConfigsRaw raw = http.request(OP_AVAILABLE_PACKAGES)
                .get(ApiPaths.CLASSIFIEDS_PACKAGES)
                .query(Query.create().add(PARAM_CATEGORY_ID, categoryId))
                .fetch(ClassifiedPackageConfigsRaw.class);
        return ClassifiedPackage.listFrom(raw);
    }

    @Override
    public ClassifiedPackage getPackage(String packageId) {
        Objects.requireNonNull(packageId, ERR_PACKAGE_ID_NULL);
        ClassifiedPackageConfigRaw raw = http.request(OP_GET_PACKAGE)
                .get(ApiPaths.classifiedsPackage(packageId))
                .fetch(ClassifiedPackageConfigRaw.class);
        return ClassifiedPackage.from(raw);
    }

    @Override
    public OfferClassifieds packagesOfOffer(String offerId) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        ClassifiedResponseRaw raw = http.request(OP_PACKAGES_OF_OFFER)
                .get(ApiPaths.offerClassifiedsPackages(offerId))
                .fetch(ClassifiedResponseRaw.class);
        return OfferClassifieds.from(raw);
    }

    @Override
    public void assignPackages(String offerId, ClassifiedAssignment assignment) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID_NULL);
        Objects.requireNonNull(assignment, ERR_ASSIGNMENT_NULL);
        http.request(OP_ASSIGN_PACKAGES)
                .put(ApiPaths.offerClassifiedsPackages(offerId))
                .jsonBody(ClassifiedsMapper.toRaw(assignment))
                .send();
    }

    @Override
    public List<OfferClassifiedStats> offerStats(List<String> offerIds, ClassifiedStatsFilter filter) {
        Objects.requireNonNull(offerIds, ERR_OFFER_IDS_NULL);
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        if (offerIds.isEmpty() || offerIds.size() > MAX_OFFER_IDS) {
            throw new IllegalArgumentException(ERR_OFFER_IDS_RANGE);
        }
        OfferStatsResponseDtoRaw raw = http.request(OP_OFFER_STATS)
                .get(ApiPaths.CLASSIFIED_OFFERS_STATS)
                .query(Query.create()
                        .addAll(PARAM_OFFER_ID, offerIds)
                        .add(PARAM_DATE_GTE, filter.eventsFrom())
                        .add(PARAM_DATE_LTE, filter.eventsTo()))
                .fetch(OfferStatsResponseDtoRaw.class);
        return OfferClassifiedStats.listFrom(raw);
    }

    @Override
    public SellerClassifiedStats sellerStats(ClassifiedStatsFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        SellerOfferStatsResponseDtoRaw raw = http.request(OP_SELLER_STATS)
                .get(ApiPaths.CLASSIFIED_SELLER_STATS)
                .query(Query.create()
                        .add(PARAM_DATE_GTE, filter.eventsFrom())
                        .add(PARAM_DATE_LTE, filter.eventsTo()))
                .fetch(SellerOfferStatsResponseDtoRaw.class);
        return SellerClassifiedStats.from(raw);
    }
}
