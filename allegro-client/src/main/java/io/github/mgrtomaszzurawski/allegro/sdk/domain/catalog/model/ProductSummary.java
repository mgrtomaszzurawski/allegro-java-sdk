/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BaseSaleProductResponseDtoPublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BaseSaleProductResponseDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImageUrlRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A product as it appears in a search result — reached via
 * {@code catalog().products().search(...)}.
 *
 * <p>The summary carries what a result list needs to display and pick from,
 * including the publication status so a consumer can tell a listable product
 * from a merely proposed one. The full product read (all parameters,
 * description, compatibility) lands next in this bucket.
 *
 * @param id the product id
 * @param name the product name, localized per the request's language
 * @param categoryId the id of the category the product is classified under, or
 *     {@code null} when the response omits it
 * @param publicationStatus the product's catalogue publication status (e.g.
 *     {@code LISTED} / {@code PROPOSED}), or {@code null} when absent
 * @param imageUrls the product image URLs, in the order Allegro returns them;
 *     never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record ProductSummary(
        String id,
        String name,
        @Nullable String categoryId,
        @Nullable String publicationStatus,
        List<String> imageUrls) {

    public ProductSummary {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    /** Map a generated Layer-1 search-result DTO to the public record. */
    public static ProductSummary from(BaseSaleProductResponseDtoRaw raw) {
        List<String> imageUrls = raw.getImages() == null ? List.of()
                : raw.getImages().stream()
                        .map(ImageUrlRaw::getUrl)
                        .filter(Objects::nonNull)
                        .toList();
        // `category` is a spec-required field; trust the contract (its nested id
        // is optional, so categoryId stays nullable).
        return new ProductSummary(
                raw.getId(),
                raw.getName(),
                raw.getCategory().getId(),
                publicationStatusOf(raw.getPublication()),
                imageUrls);
    }

    private static @Nullable String publicationStatusOf(
            @Nullable BaseSaleProductResponseDtoPublicationRaw publication) {
        if (publication == null || publication.getStatus() == null) {
            return null;
        }
        return publication.getStatus().getValue();
    }
}
