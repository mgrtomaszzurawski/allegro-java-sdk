/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgePricesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeSubsidyPricesRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * The prices attached to an active {@link Badge}: the reference market price, the
 * optional bargain (promotional) price shown to buyers, and — for subsidised
 * campaigns (e.g. Allegro Prices) — the target price and the seller's declared
 * price after the subsidy.
 *
 * <p>Each field is {@code null} when the corresponding price is absent for the
 * badge (a non-subsidised badge carries no subsidy prices).
 *
 * @param market             reference market price, or {@code null}
 * @param bargain            promotional price shown to buyers, or {@code null}
 * @param subsidyTargetPrice subsidy target price, or {@code null}
 * @param subsidySellerPrice seller's price under the subsidy, or {@code null}
 *
 * @since 0.2.0
 */
public record BadgePrices(
        @Nullable Money market,
        @Nullable Money bargain,
        @Nullable Money subsidyTargetPrice,
        @Nullable Money subsidySellerPrice) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    static BadgePrices from(BadgePricesRaw raw) {
        BadgeSubsidyPricesRaw subsidy = raw.getSubsidy();
        return new BadgePrices(
                market(raw),
                bargain(raw),
                subsidyTarget(subsidy),
                subsidySeller(subsidy));
    }

    private static @Nullable Money market(BadgePricesRaw raw) {
        return raw.getMarket() == null
                ? null
                : CampaignMappers.nullableMoney(raw.getMarket().getAmount(), raw.getMarket().getCurrency());
    }

    private static @Nullable Money bargain(BadgePricesRaw raw) {
        return raw.getBargain() == null
                ? null
                : CampaignMappers.nullableMoney(raw.getBargain().getAmount(), raw.getBargain().getCurrency());
    }

    private static @Nullable Money subsidyTarget(@Nullable BadgeSubsidyPricesRaw subsidy) {
        return subsidy == null || subsidy.getTargetPrice() == null
                ? null
                : CampaignMappers.nullableMoney(
                        subsidy.getTargetPrice().getAmount(), subsidy.getTargetPrice().getCurrency());
    }

    private static @Nullable Money subsidySeller(@Nullable BadgeSubsidyPricesRaw subsidy) {
        return subsidy == null || subsidy.getSellerPrice() == null
                ? null
                : CampaignMappers.nullableMoney(
                        subsidy.getSellerPrice().getAmount(), subsidy.getSellerPrice().getCurrency());
    }
}
