/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Marketplace;
import java.util.List;

/**
 * Read-shape probe for bucket D's starter slice ({@code client.marketplaces().list()}).
 *
 * <p>Marketplace metadata is public, so this scenario runs on an app-only
 * client-credentials grant — no stored user token needed. It verifies the live
 * response SHAPE (TESTING.md §2 read-only rule): the codes a consumer relies on
 * arrive parseable and {@code allegro-pl} is present with a base currency.
 * Output is status-level only.
 */
public final class MarketplacesDemo {

    private static final String MARKETPLACE_PL = "allegro-pl";

    private MarketplacesDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        // Public data: the account context (seller/buyer) is irrelevant here.
        System.out.println("marketplaces probe (account context '" + account
                + "' not required for public data)");
        ClientCredentials credentials = new ClientCredentials(clientId, clientSecret);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<Marketplace> marketplaces = client.marketplaces().list();
            System.out.println("marketplaces(): count=" + marketplaces.size());
            marketplaces.stream()
                    .filter(marketplace -> MARKETPLACE_PL.equals(marketplace.id()))
                    .findFirst()
                    .ifPresentOrElse(
                            poland -> System.out.println("  allegro-pl: baseCurrency="
                                    + poland.baseCurrency()
                                    + ", offerCreationLanguages=" + poland.offerCreationLanguages().size()
                                    + ", shippingCountries=" + poland.shippingCountries().size()),
                            () -> System.out.println("  WARN: allegro-pl not present in response"));
        }
    }
}
