/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder.BillingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingType;
import java.time.OffsetDateTime;

/**
 * Compile-only twin of the {@code docs/billing.md} snippets — if the documented
 * consumer code stops compiling, this module breaks the build.
 */
public final class BillingExample {

    private BillingExample() {
    }

    static long recentEntryCount(String clientId, String clientSecret) {
        var credentials = new ClientCredentials(clientId, clientSecret);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            for (BillingType type : client.billing().types()) {
                System.out.println(type.id() + " → " + type.description());
            }
            BillingFilter lastMonth = BillingFilter.builder()
                    .occurredFrom(OffsetDateTime.now().minusDays(30))
                    .build();
            return client.billing().streamEntries(lastMonth).limit(200).count();
        }
    }
}
