/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyUpdateRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnCostCoveredBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyAvailability;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.io.IOException;

/**
 * Bucket-K write→read verification (TESTING.md §2): create a seller return-policy
 * definition through the SDK, read it back, update it, then delete it and verify
 * it is gone. Return policies (unlike warranties) support DELETE, so this probe
 * self-cleans. Seller-only — no buyer seeding needed.
 *
 * <p>Run:
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=settings-return-policy -Pdemo.account=seller}.
 * Output is status-level only (operation + id + round-trip flag).
 */
final class SettingsReturnPolicyDemo {

    static final String SCENARIO = "settings-return-policy";

    private static final String DEMO_PREFIX = "[K-demo] ";
    private static final String DEMO_NAME = DEMO_PREFIX + "return policy";
    private static final String UPDATED_WITHDRAWAL_PERIOD = "P30D";
    private static final String INITIAL_WITHDRAWAL_PERIOD = "P14D";
    private static final String MSG_NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;

    private SettingsReturnPolicyDemo() {
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
                runCycle(client.settings().afterSale());
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

    private static void runCycle(AfterSaleConditions afterSale) {
        ReturnPolicy created = afterSale.createReturnPolicy(createRequest());
        System.out.println("created return policy: id=" + created.id());

        ReturnPolicy readBack = afterSale.returnPolicy(created.id());
        boolean createOk = DEMO_NAME.equals(readBack.name())
                && INITIAL_WITHDRAWAL_PERIOD.equals(readBack.withdrawalPeriod());

        ReturnPolicy updated = afterSale.updateReturnPolicy(created.id(), updateRequest());
        boolean updateOk = UPDATED_WITHDRAWAL_PERIOD.equals(updated.withdrawalPeriod());

        afterSale.deleteReturnPolicy(created.id());
        boolean deleteOk = afterSale.streamReturnPolicies()
                .noneMatch(policy -> policy.id().equals(created.id()));

        System.out.println("round-trip-ok=" + (createOk && updateOk && deleteOk)
                + " (create=" + createOk + ", update=" + updateOk + ", delete=" + deleteOk + ")");
    }

    private static ReturnPolicyRequest createRequest() {
        return ReturnPolicyRequest.builder()
                .name(DEMO_NAME)
                .fulfillment(false)
                .availability(ReturnPolicyAvailability.full())
                .withdrawalPeriod(INITIAL_WITHDRAWAL_PERIOD)
                .returnCost(ReturnCostCoveredBy.SELLER)
                .address(new AfterSalesAddress(
                        "Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL"))
                .build();
    }

    private static ReturnPolicyUpdateRequest updateRequest() {
        return ReturnPolicyUpdateRequest.builder()
                .name(DEMO_NAME)
                .availability(ReturnPolicyAvailability.full())
                .withdrawalPeriod(UPDATED_WITHDRAWAL_PERIOD)
                .returnCost(ReturnCostCoveredBy.SELLER)
                .address(new AfterSalesAddress(
                        "Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL"))
                .build();
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
