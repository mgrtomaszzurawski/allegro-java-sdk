/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/campaigns.md} badge-campaigns snippet — if
 * the documented API stops compiling, this module breaks the build.
 */
public final class BadgeCampaignsExample {

    private BadgeCampaignsExample() {
    }

    static List<BadgeCampaign> eligibleCampaigns(AllegroCredentials credentials, String marketplaceId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
            return client.campaigns().badges().availableCampaigns(marketplaceId).stream()
                    .filter(BadgeCampaign::eligible)
                    .toList();
        }
    }
}
