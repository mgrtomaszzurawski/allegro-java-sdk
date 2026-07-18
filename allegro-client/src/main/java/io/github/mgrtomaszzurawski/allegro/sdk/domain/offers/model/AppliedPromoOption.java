/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A promotion package currently applied to an offer, with its validity window —
 * part of {@link OfferPromoOptions}.
 *
 * @param id            the package identifier
 * @param validFrom     when the package became active, or {@code null}
 * @param validTo       when the package expires, or {@code null}
 * @param nextCycleDate when the next billing cycle starts, or {@code null}
 * @since 0.2.0
 */
public record AppliedPromoOption(
        @Nullable String id,
        @Nullable OffsetDateTime validFrom,
        @Nullable OffsetDateTime validTo,
        @Nullable OffsetDateTime nextCycleDate) {

    /** Project a generated applied-promo-option onto the consumer record. */
    public static AppliedPromoOption from(OfferPromoOptionRaw raw) {
        return new AppliedPromoOption(
                raw.getId(), raw.getValidFrom(), raw.getValidTo(), raw.getNextCycleDate());
    }
}
