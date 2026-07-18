/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.io.IOException;
import java.util.Optional;

/**
 * Bucket-K write→read verification (TESTING.md §2): create (or reuse) a seller
 * implied-warranty (rękojmia) definition through the SDK, then read it back and
 * assert the round-trip. Seller-only — no buyer seeding needed.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-implied-warranty -Pdemo.account=seller}.
 * Output is status-level only (operation + id + round-trip flag).
 */
final class SettingsImpliedWarrantyDemo {

    static final String SCENARIO = "settings-implied-warranty";

    private static final String DEMO_PREFIX = "[K-demo] ";
    private static final String DEMO_NAME = DEMO_PREFIX + "implied warranty";
    private static final String DEMO_DESCRIPTION =
            DEMO_PREFIX + "created by the settings-implied-warranty probe";
    // The implied-warranty period accepts whole years only (spec: 'P2Y').
    private static final String INDIVIDUAL_PERIOD = "P2Y";
    private static final String CORPORATE_PERIOD = "P1Y";
    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;

    private SettingsImpliedWarrantyDemo() {
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
            try {
                AfterSaleConditions afterSale = client.settings().afterSale();
                ImpliedWarranty written = createOrUpdateDemo(afterSale);
                ImpliedWarranty readBack = afterSale.impliedWarranty(written.id());
                boolean roundTrip = DEMO_NAME.equals(readBack.name());
                System.out.println("read-back: id=" + readBack.id()
                        + ", round-trip-ok=" + roundTrip);
            } catch (AllegroBadRequestException rejection) {
                printFieldErrors(rejection);
                throw rejection;
            } finally {
                // Allegro rotates the refresh token on EVERY refresh; persist it
                // even on failure, or the shared store is left with a dead token.
                persistRotatedToken(tokenStore, account, client);
            }
        }
    }

    private static ImpliedWarranty createOrUpdateDemo(AfterSaleConditions afterSale) {
        ImpliedWarrantyRequest request = ImpliedWarrantyRequest.builder()
                .name(DEMO_NAME)
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .corporate(ImpliedWarrantyPeriod.of(CORPORATE_PERIOD))
                .address(new AfterSalesAddress(
                        "Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL"))
                .description(DEMO_DESCRIPTION)
                .build();
        // No DELETE for implied warranties either — reuse a single demo definition.
        Optional<ImpliedWarrantySummary> existing = afterSale.streamImpliedWarranties()
                .filter(summary -> DEMO_NAME.equals(summary.name()))
                .findFirst();
        if (existing.isPresent()) {
            ImpliedWarranty updated = afterSale.updateImpliedWarranty(existing.get().id(), request);
            System.out.println("updated implied warranty: id=" + updated.id());
            return updated;
        }
        ImpliedWarranty created = afterSale.createImpliedWarranty(request);
        System.out.println("created implied warranty: id=" + created.id());
        return created;
    }

    private static void printFieldErrors(AllegroBadRequestException rejection) {
        for (AllegroFieldError fieldError : rejection.errors()) {
            System.out.println("field-error: code=" + fieldError.code()
                    + ", path=" + fieldError.path()
                    + ", message=" + fieldError.message());
        }
    }

    private static void persistRotatedToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
