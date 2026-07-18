/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RefundDispositionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.StockFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.StockItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.TaxId;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Compile-only twin of the {@code docs/fulfillment.md} snippets — if the doc's
 * code stops compiling, this module breaks the build.
 */
public final class FulfillmentExample {

    private static final int LOW_STOCK_DAYS = 14;
    private static final int RECENT_DAYS = 30;

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

    static List<StockItem> lowStock(AllegroClient client) {
        try (Stream<StockItem> stock = client.fulfillment().stock(
                StockFilter.builder().outOfStockInTo(LOW_STOCK_DAYS).sort("outOfStockIn").build())) {
            return stock.toList();
        }
    }

    static long recentDispositionCount(AllegroClient client) {
        try (Stream<?> report = client.fulfillment().refundDispositions(
                RefundDispositionFilter.builder()
                        .createdFrom(OffsetDateTime.now().minusDays(RECENT_DAYS))
                        .build())) {
            return report.count();
        }
    }

    static TaxId registerTaxId(AllegroClient client, String taxId) {
        client.fulfillment().addTaxId(taxId);
        return client.fulfillment().taxId();
    }
}
