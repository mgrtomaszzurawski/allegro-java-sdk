/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ImageUrlRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductProposalsResponsePublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductProposalsResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The result of proposing a new product ({@code catalog().products().propose(...)}):
 * the assigned proposal id and its current moderation {@link #status()}, echoed back
 * with the proposed name, category and images.
 *
 * <p>The proposed parameters and standardized description are not read back yet
 * (field-depth follow-up).
 *
 * @param id the proposal id
 * @param name the proposed product name
 * @param categoryId the category the product was proposed in, or {@code null}
 * @param imageUrls the proposed image URLs (possibly empty)
 * @param language the listing language, or {@code null}
 * @param status the moderation status
 * @since 0.2.0
 */
public record ProductProposal(
        @Nullable String id,
        @Nullable String name,
        @Nullable String categoryId,
        List<String> imageUrls,
        @Nullable String language,
        ProductProposalStatus status) {

    /** Defensively copies the image list. */
    public ProductProposal {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    /** Map the generated Layer-1 response DTO onto the domain record. */
    public static ProductProposal from(ProductProposalsResponseRaw raw) {
        String categoryId = raw.getCategory() == null ? null : raw.getCategory().getId();
        List<String> images = raw.getImages() == null
                ? List.of()
                : raw.getImages().stream().map(ImageUrlRaw::getUrl).filter(url -> url != null).toList();
        return new ProductProposal(
                raw.getId(), raw.getName(), categoryId, images, raw.getLanguage(),
                statusOf(raw.getPublication()));
    }

    private static ProductProposalStatus statusOf(@Nullable ProductProposalsResponsePublicationRaw publication) {
        if (publication == null || publication.getStatus() == null) {
            return ProductProposalStatus.UNKNOWN;
        }
        return ProductProposalStatus.from(publication.getStatus().getValue());
    }
}
