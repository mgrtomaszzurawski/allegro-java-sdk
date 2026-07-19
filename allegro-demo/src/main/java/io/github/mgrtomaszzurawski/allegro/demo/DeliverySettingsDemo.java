/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.DeliverySettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsView;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.JoinStrategy;
import java.io.IOException;

/**
 * Bucket C live probe (TESTING.md §2) for {@code shipping().settings()}. Verifies
 * both the read mapping and the write contract with an <em>idempotent</em>
 * write→read: read the current settings, PUT them back unchanged, read again and
 * assert the join policy round-trips. Re-sending the current state avoids
 * mutating the seller's real configuration while still exercising the write path
 * that surfaces spec-vs-wire divergences.
 *
 * <p>Needs the seller user token ({@code sale:settings:*}). Status-level output
 * only — never bodies or tokens.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=delivery-settings -Pdemo.account=seller
 * </pre>
 */
public final class DeliverySettingsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "delivery-settings";

    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_ROUND_TRIP = "join policy did not round-trip through update";

    private DeliverySettingsDemo() {
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
                ignored -> System.out.println(TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                probe(client.shipping().settings());
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void probe(DeliverySettings settings) {
        DeliverySettingsView current = settings.get();
        System.out.println("settings().get(): marketplace=" + current.marketplaceId()
                + ", joinPolicy=" + current.joinPolicy()
                + ", freeDelivery=" + current.freeDelivery()
                + ", abroadFreeDelivery=" + current.abroadFreeDelivery()
                + ", updatedAt=" + current.updatedAt());

        JoinStrategy joinPolicy = current.joinPolicy();
        if (joinPolicy == null || joinPolicy == JoinStrategy.UNKNOWN) {
            System.out.println("(join policy absent/unmodelled - skipping idempotent write check)");
            return;
        }

        DeliverySettingsView updated = settings.update(DeliverySettingsRequest.builder()
                .marketplaceId(current.marketplaceId())
                .freeDelivery(current.freeDelivery())
                .abroadFreeDelivery(current.abroadFreeDelivery())
                .joinPolicy(joinPolicy)
                .build());
        System.out.println("settings().update() ok: joinPolicy=" + updated.joinPolicy());
        if (updated.joinPolicy() != joinPolicy) {
            throw new IllegalStateException(ERR_ROUND_TRIP);
        }
    }
}
