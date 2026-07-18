/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One offer's Allegro Prices status, returned lazily by
 * {@code allegroPrices().streamOffersStatus(...)}: its base price, whether Allegro
 * sees a discount opportunity, the recommended / declared / actual subsidy
 * reductions, and the resulting buyer price.
 *
 * <p>The reduction percentages are the seller's maximum declared percentage for
 * each stage and are {@code null} when that stage does not apply to the offer.
 * (The response's per-stage reduction objects are a generated {@code oneOf}; the
 * SDK reads them from the raw JSON to avoid the generator's over-matching
 * deserializer — see {@code KNOWN-SERVER-BEHAVIORS.md} / BACKLOG Phase 1.1.)
 *
 * @param offerId                        the offer
 * @param name                           the offer's name
 * @param marketplaceId                  the marketplace this status is for
 * @param basePrice                      the offer's base price, or {@code null}
 * @param discountOpportunity            {@code true} if Allegro sees a discount opportunity
 * @param recommendedReductionPercentage recommended max declared reduction %, or {@code null}
 * @param declaredReductionPercentage    the seller's declared max reduction %, or {@code null}
 * @param actualReductionPercentage      the currently applied max reduction %, or {@code null}
 * @param finalBuyerPrice                the price the buyer pays after the subsidy, or {@code null}
 * @param discountedAt                   when the offer entered the discount, or {@code null}
 * @param excludedAt                     when the offer was excluded, or {@code null}
 *
 * @since 0.2.0
 */
public record AllegroPricesOfferStatus(
        String offerId,
        String name,
        String marketplaceId,
        @Nullable Money basePrice,
        boolean discountOpportunity,
        @Nullable String recommendedReductionPercentage,
        @Nullable String declaredReductionPercentage,
        @Nullable String actualReductionPercentage,
        @Nullable Money finalBuyerPrice,
        @Nullable OffsetDateTime discountedAt,
        @Nullable OffsetDateTime excludedAt) {
}
