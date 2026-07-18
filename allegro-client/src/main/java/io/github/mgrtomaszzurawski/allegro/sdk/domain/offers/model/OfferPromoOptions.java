/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The promotion packages currently applied to one offer — reached from
 * {@code offers().promoOptions().forOffer(offerId)}.
 *
 * @param offerId       the offer these options belong to
 * @param basePackage   the applied base package, or {@code null} if none
 * @param extraPackages the applied add-on packages
 * @since 0.2.0
 */
public record OfferPromoOptions(
        @Nullable String offerId,
        @Nullable AppliedPromoOption basePackage,
        List<AppliedPromoOption> extraPackages) {

    public OfferPromoOptions {
        extraPackages = List.copyOf(extraPackages);
    }

    /** Project the generated offer-promo-options response onto the consumer record. */
    public static OfferPromoOptions from(OfferPromoOptionsRaw raw) {
        OfferPromoOptionRaw base = raw.getBasePackage();
        List<OfferPromoOptionRaw> extras = raw.getExtraPackages();
        return new OfferPromoOptions(
                raw.getOfferId(),
                base == null ? null : AppliedPromoOption.from(base),
                extras == null ? List.of() : extras.stream().map(AppliedPromoOption::from).toList());
    }
}
