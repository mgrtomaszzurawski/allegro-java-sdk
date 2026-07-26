/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceOfferPromoOptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The promotion packages applied to one offer on a single additional marketplace —
 * a value of {@link OfferPromoOptions#additionalMarketplaces()} (keyed by marketplace id).
 *
 * @param basePackage   the applied base package on this marketplace, or {@code null}
 * @param extraPackages the applied add-on packages on this marketplace
 * @param pendingChanges the changes queued for the next cycle on this marketplace,
 *     or {@code null} if none are pending
 * @since 0.7.0
 */
public record MarketplacePromoOptions(
        @Nullable AppliedPromoOption basePackage,
        List<AppliedPromoOption> extraPackages,
        @Nullable PendingPromoChanges pendingChanges) {

    public MarketplacePromoOptions {
        extraPackages = List.copyOf(extraPackages);
    }

    /** Project a generated per-marketplace promo-option block onto the consumer record. */
    public static MarketplacePromoOptions from(MarketplaceOfferPromoOptionRaw raw) {
        OfferPromoOptionRaw base = raw.getBasePackage();
        List<OfferPromoOptionRaw> extras = raw.getExtraPackages();
        return new MarketplacePromoOptions(
                base == null ? null : AppliedPromoOption.from(base),
                extras == null ? List.of() : extras.stream().map(AppliedPromoOption::from).toList(),
                PendingPromoChanges.from(raw.getPendingChanges()));
    }
}
