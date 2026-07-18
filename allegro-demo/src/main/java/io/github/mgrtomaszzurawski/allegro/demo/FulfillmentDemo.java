/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.Fulfillment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RefundDispositionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.TaxId;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import java.io.IOException;

/**
 * Sandbox probe for the fulfillment facade: reads the seller's active removal
 * preference, writes it back unchanged, and reads again to prove the write→read
 * cycle through the SDK (TESTING.md §2); then reads the stock / available-product
 * / refund-disposition reports and the tax id. Writing the current preference
 * value back keeps the seller's configuration untouched; the reports are
 * read-only and only counts (never product names, buyer logins or the tax
 * number) are logged.
 *
 * <p>One Fulfillment is an enrolled-account service; on a non-enrolled sandbox
 * seller the calls are rejected, and the probe then verifies the typed-exception
 * path instead of a data round-trip — a real observation of the wire either way.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=fulfillment -Pdemo.account=seller
 * </pre>
 */
public final class FulfillmentDemo {

    /** Scenario key under which {@link DemoApp} registers this probe. */
    public static final String SCENARIO = "fulfillment";

    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final int EXIT_NO_TOKEN = 2;
    /** Cap on report rows walked — a probe, not a load test (TESTING.md §2). */
    private static final int REPORT_SAMPLE = 5;

    private FulfillmentDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            System.exit(EXIT_NO_TOKEN);
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                writeReadRoundTrip(client.fulfillment());
                readReports(client.fulfillment());
            } catch (AllegroException failure) {
                System.out.println("fulfillment call failed (status " + failure.statusCode()
                        + ") - the seller is most likely not enrolled in One Fulfillment; "
                        + "verified the typed-exception path");
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void writeReadRoundTrip(Fulfillment fulfillment) {
        RemovalPreference before = fulfillment.removalPreference();
        System.out.println("read removal preference: operation=" + before.operation());
        fulfillment.setRemovalPreference(before);
        RemovalPreference after = fulfillment.removalPreference();
        System.out.println("write->read round-trip operation match: "
                + (after.operation() == before.operation()));
    }

    private static void readReports(Fulfillment fulfillment) {
        long stockLines = fulfillment.stock().limit(REPORT_SAMPLE).count();
        System.out.println("stock lines (first " + REPORT_SAMPLE + "): " + stockLines);
        long products = fulfillment.availableProducts().limit(REPORT_SAMPLE).count();
        System.out.println("available products (first " + REPORT_SAMPLE + "): " + products);
        long dispositions = fulfillment.refundDispositions(RefundDispositionFilter.all())
                .limit(REPORT_SAMPLE).count();
        System.out.println("refund dispositions (first " + REPORT_SAMPLE + "): " + dispositions);
        TaxId taxId = fulfillment.taxId();
        System.out.println("tax id present: " + (taxId.taxId() != null)
                + ", verification status: " + taxId.verificationStatus());
    }
}
