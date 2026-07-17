/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;

/**
 * Compile-only twin of the {@code docs/fulfillment.md} snippet — if the doc's
 * code stops compiling, this module breaks the build.
 */
public final class FulfillmentExample {

    private FulfillmentExample() {
    }

    static RemovalPreference setWithdrawalPreference(String clientId, String clientSecret) {
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));

        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
            WithdrawalAddress returnAddress = WithdrawalAddress.builder()
                    .company("My Store Ltd")
                    .street("Testowa 1")
                    .postalCode("00-001")
                    .city("Warszawa")
                    .countryCode("PL")
                    .phone(PhoneNumber.of("48", "600100200"))
                    .build();

            RemovalPreference preference = RemovalPreference.builder()
                    .operation(RemovalOperation.WITHDRAWAL)
                    .withdrawalAddress(returnAddress)
                    .build();

            return client.fulfillment().setRemovalPreference(preference);
        }
    }
}
