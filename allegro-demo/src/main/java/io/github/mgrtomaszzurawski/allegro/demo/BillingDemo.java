/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingType;
import java.util.List;

/**
 * Live sandbox probe for the billing facade (TESTING.md §2, read-shape
 * verification). {@code billing().types()} declares no OAuth scope, so this runs
 * with an app-only client-credentials token — no seller device token needed:
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=billing-types
 * </pre>
 *
 * Output is status-level only — the dictionary carries no personal data.
 */
public final class BillingDemo {

    /** Scenario name registered in {@link DemoApp}. */
    public static final String SCENARIO = "billing-types";

    private static final int PRINT_LIMIT = 5;

    private BillingDemo() {
    }

    /** Entry point registered in {@link DemoApp}. */
    public static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            List<BillingType> types = client.billing().types();
            System.out.println("billing().types(): " + types.size() + " billing type(s) read");
            types.stream().limit(PRINT_LIMIT).forEach(type ->
                    System.out.println("  " + type.id() + " -> " + type.description()));
            System.out.println("Shape OK: the billing-type dictionary mapped without error.");
        }
    }
}
