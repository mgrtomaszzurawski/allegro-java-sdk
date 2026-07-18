/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgePatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperation;

/**
 * Compile-only twin of the {@code docs/campaigns.md} apply/update snippets — if the
 * documented badge write API stops compiling, this module breaks the build.
 */
public final class BadgeApplicationExample {

    private BadgeApplicationExample() {
    }

    static BadgeApplication applyForBadge(AllegroCredentials credentials, String campaignId,
            String offerId, Money bargainPrice) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
            BadgeApplicationRequest request = BadgeApplicationRequest.builder()
                    .campaignId(campaignId)
                    .offerId(offerId)
                    .bargainPrice(bargainPrice)
                    .build();
            return client.campaigns().badges().apply(request);
        }
    }

    static BadgeOperation finishBadge(AllegroCredentials credentials, String offerId, String campaignId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
            return client.campaigns().badges().update(offerId, campaignId, BadgePatch.finish());
        }
    }
}
