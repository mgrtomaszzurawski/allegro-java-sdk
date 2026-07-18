/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryOptionsDtoRaw;
import org.jspecify.annotations.Nullable;

/**
 * What a category permits — the flags Allegro returns alongside a
 * {@link Category}.
 *
 * @param advertisement offers of type {@code ADVERTISEMENT} may be listed here
 * @param offersWithProductPublicationEnabled offers may be assigned to a product
 * @param productCreationEnabled new products may be created in this category
 * @param sellerCanRequirePurchaseComments the category supports a required
 *     message-to-seller
 *
 * @since 0.2.0
 */
public record CategoryOptions(
        boolean advertisement,
        boolean offersWithProductPublicationEnabled,
        boolean productCreationEnabled,
        boolean sellerCanRequirePurchaseComments) {

    /**
     * Map the generated Layer-1 DTO, or {@code null} when the category carries
     * no options block.
     */
    static @Nullable CategoryOptions from(@Nullable CategoryOptionsDtoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new CategoryOptions(
                Boolean.TRUE.equals(raw.getAdvertisement()),
                Boolean.TRUE.equals(raw.getOffersWithProductPublicationEnabled()),
                Boolean.TRUE.equals(raw.getProductCreationEnabled()),
                Boolean.TRUE.equals(raw.getSellerCanRequirePurchaseComments()));
    }
}
