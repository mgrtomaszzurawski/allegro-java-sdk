/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.LineItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;

/**
 * Compile-only twin of the {@code docs/orders.md} fetch-an-order snippet — if
 * the documented consumer code stops compiling, this module breaks the build.
 */
public final class OrdersExample {

    private OrdersExample() {
    }

    static Order fetchOrder(String clientId, String clientSecret, String orderId) {
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));

        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            Order order = client.orders().get(orderId);
            System.out.println("Status: " + order.status() + " / seller: " + order.sellerStatus());
            System.out.println("To pay: " + order.totalToPay().amount()
                    + " " + order.totalToPay().currency());
            for (LineItem item : order.lineItems()) {
                System.out.println(item.quantity() + " x " + item.offerName());
            }
            return order;
        }
    }
}
