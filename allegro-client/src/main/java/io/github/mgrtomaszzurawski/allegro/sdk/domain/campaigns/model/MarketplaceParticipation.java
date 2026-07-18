/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AccountParticipationMarketplaceRaw;

/**
 * The seller account's Allegro Prices participation on a single marketplace.
 *
 * @param marketplaceId the marketplace (e.g. {@code "allegro-pl"})
 * @param status        whether the account participates there
 *
 * @since 0.2.0
 */
public record MarketplaceParticipation(String marketplaceId, ParticipationStatus status) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    static MarketplaceParticipation from(AccountParticipationMarketplaceRaw raw) {
        return new MarketplaceParticipation(raw.getId(), ParticipationStatus.from(raw.getStatus()));
    }
}
