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
 * classification (id plus the full {@link #categoryPath() category path}), images,
 * the {@link #parameters() parameter values} that describe it, the
 * {@link #offerRequirements() offer requirements} it imposes, and the
 * {@link #productSafety() GPSR product-safety} information. The structured
 * {@code description}, compatibility list, and trusted/AI-co-created content blocks
 * are still omitted and can be added later without breaking read consumers.
 *
 * @param id the product id
 * @param name the product name, in the account's default language (a per-call
 *     language option is not yet wired)
 * @param categoryId the id of the category the product is classified under, or
 *     {@code null} when the category carries no id
 * @param categoryPath the category path from the root down to the product's
 *     category, in order; never {@code null}, possibly empty
 * @param publicationStatus the catalogue publication status (e.g. {@code LISTED}
 *     / {@code PROPOSED}), or {@code null} when absent
 * @param hasProtectedBrand whether the product carries a brand-protection
 *     restriction
 * @param imageUrls the product image URLs, in order; never {@code null}
 * @param parameters the parameter values describing the product, in order; never
 *     {@code null}, possibly empty
 * @param offerRequirements the requirements the product imposes on offers built
 *     from it, or {@code null} when absent
 * @param productSafety the GPSR product-safety information, or {@code null} when
 *     absent
 *
 * @since 0.2.0
 */
public record Product(
        String id,
        String name,
        @Nullable String categoryId,
        List<ProductCategoryPathElement> categoryPath,
        @Nullable String publicationStatus,
        boolean hasProtectedBrand,
        List<String> imageUrls,
        List<ProductParameterValue> parameters,
        @Nullable OfferRequirements offerRequirements,
        @Nullable ProductSafety productSafety) {

    public Product {
        categoryPath = categoryPath == null ? List.of() : List.copyOf(categoryPath);
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
                categoryPathOf(raw),
                publicationStatusOf(raw),
                Boolean.TRUE.equals(raw.getHasProtectedBrand()),
                imageUrls,
                parameters,
                OfferRequirements.from(raw.getOfferRequirements()),
                ProductSafety.from(raw.getProductSafety()));
    }

    private static List<ProductCategoryPathElement> categoryPathOf(SaleProductDtoRaw raw) {
        if (raw.getCategory().getPath() == null) {
            return List.of();
        }
        return raw.getCategory().getPath().stream().map(ProductCategoryPathElement::from).toList();
    }

    private static @Nullable String publicationStatusOf(SaleProductDtoRaw raw) {
        SaleProductDtoPublicationRaw publication = raw.getPublication();
        if (publication == null || publication.getStatus() == null) {
            return null;
        }
        return publication.getStatus().getValue();
    }
}
