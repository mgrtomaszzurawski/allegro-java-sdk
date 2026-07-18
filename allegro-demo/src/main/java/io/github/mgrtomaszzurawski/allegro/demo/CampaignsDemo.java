/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import java.io.IOException;
import java.util.List;

/**
 * Sandbox read-shape probe for bucket H (campaigns), run via
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=campaigns -Pdemo.account=seller}.
 *
 * <p>Reads badge campaigns through the SDK against the live sandbox and asserts
 * the response deserializes and maps — the read-only verification prescribed by
 * TESTING.md §2 for a discovery endpoint (there is nothing to create for it).
 * The write→read cycles for the applying/subsidy commands ship with the full
 * bucket. Output is status-level only — campaign metadata, never bodies or tokens.
 */
final class CampaignsDemo {

    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String STALE_TOKEN = "(stored token expired - rerun auth-bootstrap)";

    private CampaignsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(NO_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(STALE_TOKEN),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<BadgeCampaign> campaigns = client.campaigns().badges().availableCampaigns();
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
            System.out.println("badges().availableCampaigns(): " + campaigns.size() + " campaign(s)");
            for (BadgeCampaign campaign : campaigns) {
                System.out.println("  - " + campaign.id() + " [" + campaign.type() + "] eligible="
                        + campaign.eligible());
            }
        }
    }
}
