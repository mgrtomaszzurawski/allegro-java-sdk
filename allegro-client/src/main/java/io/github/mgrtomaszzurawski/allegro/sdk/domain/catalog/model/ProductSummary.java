/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BaseSaleProductResponseDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImageUrlRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductCategoryWithPathRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A product as it appears in a search result — reached via
 * {@code catalog().products().search(...)}.
 *
 * <p>The summary carries what a result list needs to display; fetch the full
 * product (parameters, description, compatibility, …) with {@code products().get(id)}.
 *
 * @param id the product id — pass it to {@code products().get(id)} for full data
 * @param name the product name, localized per the request's language
 * @param categoryId the id of the category the product is classified under, or
 *     {@code null} when the response omits it
 * @param imageUrls the product image URLs, in the order Allegro returns them;
 *     never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record ProductSummary(
        String id,
        String name,
        @Nullable String categoryId,
        List<String> imageUrls) {

    public ProductSummary {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    /** Map a generated Layer-1 search-result DTO to the public record. */
    public static ProductSummary from(BaseSaleProductResponseDtoRaw raw) {
        ProductCategoryWithPathRaw category = raw.getCategory();
        List<String> imageUrls = raw.getImages() == null ? List.of()
                : raw.getImages().stream()
                        .map(ImageUrlRaw::getUrl)
                        .filter(Objects::nonNull)
                        .toList();
        return new ProductSummary(
                raw.getId(),
                raw.getName(),
                category == null ? null : category.getId(),
                imageUrls);
    }
}
