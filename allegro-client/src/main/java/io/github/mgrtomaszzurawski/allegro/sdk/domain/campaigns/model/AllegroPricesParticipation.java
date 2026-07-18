/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPricesAccountParticipationResponseRaw;
import java.util.List;

/**
 * The seller account's Allegro Prices participation across marketplaces, returned
 * by {@code allegroPrices().participation()} and
 * {@code allegroPrices().updateParticipation(...)}.
 *
 * @param marketplaces per-marketplace participation; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record AllegroPricesParticipation(List<MarketplaceParticipation> marketplaces) {

    public AllegroPricesParticipation {
        marketplaces = List.copyOf(marketplaces);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static AllegroPricesParticipation from(AllegroPricesAccountParticipationResponseRaw raw) {
        return new AllegroPricesParticipation(
                raw.getMarketplaces().stream().map(MarketplaceParticipation::from).toList());
    }
}
