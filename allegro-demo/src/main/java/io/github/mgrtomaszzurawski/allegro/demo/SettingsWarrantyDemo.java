/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
import java.io.IOException;
import java.util.Optional;

/**
 * Bucket-K write→read verification (TESTING.md §2): create (or reuse) a seller
 * warranty definition through the SDK, then read it back and assert the
 * round-trip. Seller-only — no buyer seeding needed.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-warranty -Pdemo.account=seller}.
 * Output is status-level only (operation + id + round-trip flag).
 */
final class SettingsWarrantyDemo {

    private static final String DEMO_PREFIX = "[K-demo] ";
    private static final String DEMO_WARRANTY_NAME = DEMO_PREFIX + "seller warranty";
    private static final String DEMO_DESCRIPTION = DEMO_PREFIX + "created by the settings-warranty probe";
    private static final String INDIVIDUAL_PERIOD = "P24M";
    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;

    private SettingsWarrantyDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(MSG_NO_TOKEN.formatted(account));
            System.exit(EXIT_NO_TOKEN);
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            AfterSaleConditions afterSale = client.settings().afterSale();
            Warranty written = createOrUpdateDemoWarranty(afterSale);
            // Read-back through the SDK and assert the round-trip.
            Warranty readBack = afterSale.warranty(written.id());
            boolean roundTrip = DEMO_WARRANTY_NAME.equals(readBack.name())
                    && readBack.type() == WarrantyType.SELLER;
            System.out.println("read-back: id=" + readBack.id()
                    + ", type=" + readBack.type()
                    + ", round-trip-ok=" + roundTrip);
            rotateStoredToken(tokenStore, account, client);
        }
    }

    private static Warranty createOrUpdateDemoWarranty(AfterSaleConditions afterSale) {
        WarrantyRequest request = WarrantyRequest.builder()
                .name(DEMO_WARRANTY_NAME)
                .type(WarrantyType.SELLER)
                .individual(WarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .description(DEMO_DESCRIPTION)
                .build();
        // Warranties have no DELETE in this API; reuse a single demo definition
        // so repeated runs do not pile up sandbox state.
        Optional<WarrantySummary> existing = afterSale.streamWarranties()
                .filter(summary -> DEMO_WARRANTY_NAME.equals(summary.name()))
                .findFirst();
        if (existing.isPresent()) {
            Warranty updated = afterSale.updateWarranty(existing.get().id(), request);
            System.out.println("updated warranty: id=" + updated.id());
            return updated;
        }
        Warranty created = afterSale.createWarranty(request);
        System.out.println("created warranty: id=" + created.id());
        return created;
    }

    private static void rotateStoredToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
