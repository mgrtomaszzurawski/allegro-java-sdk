/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsPendingChangesRaw;
import org.jspecify.annotations.Nullable;

/**
 * The promotion-package changes queued to take effect at the next billing cycle
 * for an offer — part of {@link OfferPromoOptions}.
 *
 * @param basePackage the base package that will apply after the pending change,
 *     or {@code null} if no base-package change is queued
 * @since 0.7.0
 */
public record PendingPromoChanges(@Nullable AppliedPromoOption basePackage) {

    /** Project the generated pending-changes block onto the consumer record, or {@code null}. */
    public static @Nullable PendingPromoChanges from(@Nullable OfferPromoOptionsPendingChangesRaw raw) {
        if (raw == null) {
            return null;
        }
        OfferPromoOptionRaw base = raw.getBasePackage();
        return new PendingPromoChanges(base == null ? null : AppliedPromoOption.from(base));
    }
}
