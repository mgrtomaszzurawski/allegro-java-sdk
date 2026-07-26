/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleOfferMarketplaceDetailsDTORaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleOfferMarketplaceDetailsDTOAvailabilityRaw;
import java.util.List;

/**
 * Per-marketplace availability of one offer inside a flexible bundle's slot: on a
 * given marketplace, whether the offer is currently available in the bundle and,
 * when it is not, why not.
 *
 * @param marketplaceId the marketplace identifier (e.g. {@code allegro-pl})
 * @param available whether the offer is available in the bundle on this marketplace
 * @param reasons when unavailable, the reason codes; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record FlexibleBundleOfferMarketplace(String marketplaceId, boolean available, List<String> reasons) {

    public FlexibleBundleOfferMarketplace {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    /** Map the generated Layer-1 marketplace-details DTO to the public record. */
    static FlexibleBundleOfferMarketplace from(FlexibleBundleOfferMarketplaceDetailsDTORaw raw) {
        FlexibleBundleOfferMarketplaceDetailsDTOAvailabilityRaw availability = raw.getAvailability();
        return new FlexibleBundleOfferMarketplace(
                raw.getId(),
                availability != null && Boolean.TRUE.equals(availability.getAvailable()),
                availability == null ? List.of() : availability.getReasons());
    }
}
