/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ImageUrlRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductDtoPublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductDtoRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A full product from the Allegro catalogue — reached via
 * {@code catalog().products().get(productId)}.
 *
 * <p>A product is the shared definition an offer is built from: its identity,
 * classification, images, and the parameter values that describe it. This record
 * exposes that core; the deeper blocks of the underlying DTO — the structured
 * {@code description}, compatibility list, and safety/trusted-content — are
 * intentionally omitted for now and can be added later without breaking read
 * consumers.
 *
 * @param id the product id
 * @param name the product name, in the account's default language (a per-call
 *     language option is not yet wired)
 * @param categoryId the id of the category the product is classified under, or
 *     {@code null} when the category carries no id
 * @param publicationStatus the catalogue publication status (e.g. {@code LISTED}
 *     / {@code PROPOSED}), or {@code null} when absent
 * @param hasProtectedBrand whether the product carries a brand-protection
 *     restriction
 * @param imageUrls the product image URLs, in order; never {@code null}
 * @param parameters the parameter values describing the product, in order; never
 *     {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record Product(
        String id,
        String name,
        @Nullable String categoryId,
        @Nullable String publicationStatus,
        boolean hasProtectedBrand,
        List<String> imageUrls,
        List<ProductParameterValue> parameters) {

    public Product {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /** Map the generated Layer-1 product DTO to the public record. */
    public static Product from(SaleProductDtoRaw raw) {
        List<String> imageUrls = raw.getImages() == null ? List.of()
                : raw.getImages().stream()
                        .map(ImageUrlRaw::getUrl)
                        .filter(Objects::nonNull)
                        .toList();
        List<ProductParameterValue> parameters = raw.getParameters() == null ? List.of()
                : raw.getParameters().stream().map(ProductParameterValue::from).toList();
        return new Product(
                raw.getId(),
                raw.getName(),
                // `category` is a spec-required field; trust the contract (its
                // nested id is optional, so categoryId stays nullable).
                raw.getCategory().getId(),
                publicationStatusOf(raw),
                Boolean.TRUE.equals(raw.getHasProtectedBrand()),
                imageUrls,
                parameters);
    }

    private static @Nullable String publicationStatusOf(SaleProductDtoRaw raw) {
        SaleProductDtoPublicationRaw publication = raw.getPublication();
        if (publication == null || publication.getStatus() == null) {
            return null;
        }
        return publication.getStatus().getValue();
    }
}
