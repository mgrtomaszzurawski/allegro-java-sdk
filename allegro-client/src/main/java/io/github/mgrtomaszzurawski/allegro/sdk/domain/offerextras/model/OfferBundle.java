/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferBundleDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBundlesDTORaw;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * A fixed offer bundle a seller has defined — a set of offers a buyer can buy
 * together at a per-marketplace discount.
 *
 * @param id the bundle identifier
 * @param offers the offers in the bundle; never {@code null}, possibly empty
 * @param discounts the per-marketplace discounts; never {@code null}, possibly
 *     empty
 * @param publications the per-marketplace publication statuses; never
 *     {@code null}, possibly empty
 * @param createdAt when the bundle was created
 * @param createdBy who created the bundle
 *
 * @since 0.2.0
 */
public record OfferBundle(
        String id,
        List<BundledOffer> offers,
        List<BundleDiscount> discounts,
        List<BundlePublication> publications,
        OffsetDateTime createdAt,
        BundleCreatedBy createdBy) {

    public OfferBundle {
        offers = List.copyOf(offers);
        discounts = List.copyOf(discounts);
        publications = List.copyOf(publications);
    }

    /** Map one generated Layer-1 bundle DTO to the public record. */
    public static OfferBundle from(OfferBundleDTORaw raw) {
        return new OfferBundle(
                raw.getId(),
                mapOrEmpty(raw.getOffers(), BundledOffer::from),
                mapOrEmpty(raw.getDiscounts(), BundleDiscount::from),
                mapOrEmpty(raw.getPublication(), BundlePublication::from),
                raw.getCreatedAt(),
                createdByFrom(raw.getCreatedBy()));
    }

    /** Map the generated Layer-1 list response to public records. */
    public static List<OfferBundle> listFrom(OfferBundlesDTORaw raw) {
        return mapOrEmpty(raw.getBundles(), OfferBundle::from);
    }

    private static BundleCreatedBy createdByFrom(OfferBundleDTORaw.CreatedByEnum raw) {
        try {
            return BundleCreatedBy.valueOf(raw.name());
        } catch (IllegalArgumentException unknownCreator) {
            return BundleCreatedBy.UNKNOWN;
        }
    }

    private static <R, T> List<T> mapOrEmpty(@Nullable List<R> raw, Function<R, T> mapper) {
        return raw == null ? List.of() : raw.stream().map(mapper).toList();
    }
}
