/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryOption;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryProposal;
import java.io.IOException;
import java.util.Optional;

/**
 * Bucket C live probe (TESTING.md §2) for {@code shipping().deliveryOptionsFor(orderId)}
 * (#145). Reads the delivery proposal Allegro returns for a real seller order —
 * the ready-to-submit {@code suggestedInput} shipment plus the available delivery
 * options — through the SDK, proving the response mapping against the live wire.
 *
 * <p>Picks the first {@code READY_FOR_PROCESSING} order on the account automatically;
 * override with {@code -Ddemo.orderId=<id>}. With no processable order it explains
 * what it needs and exits cleanly. Prints shapes/counts only — never PII.
 */
public final class DeliveryOptionsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "delivery-options";

    private static final String ORDER_ID_PROPERTY = "demo.orderId";
    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String NO_ORDER =
            "No READY_FOR_PROCESSING order found and no -Ddemo.orderId set - seed/await a buyer order first";

    private DeliveryOptionsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(NO_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            try {
                probe(client);
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void probe(AllegroClient client) {
        String orderId = resolveOrderId(client);
        if (orderId == null) {
            System.out.println(NO_ORDER);
            return;
        }
        System.out.println("deliveryOptionsFor(order " + orderId + ")");
        DeliveryProposal proposal = client.shipping().deliveryOptionsFor(orderId);
        System.out.println("  suggestedInput present : " + (proposal.suggestedInput() != null));
        System.out.println("  deliveryOptions        : " + proposal.deliveryOptions().size());
        for (DeliveryOption option : proposal.deliveryOptions()) {
            System.out.println("    - " + option.deliveryType()
                    + " / pay=" + option.paymentType()
                    + " / pkg=" + option.packageType()
                    + " / limits=" + (option.limits() != null));
        }
        System.out.println("Shape OK: the delivery proposal mapped without error.");
    }

    private static String resolveOrderId(AllegroClient client) {
        String override = System.getProperty(ORDER_ID_PROPERTY);
        if (override != null && !override.isBlank()) {
            return override;
        }
        Optional<Order> processable = client.orders().streamOrders(OrderFilter.all())
                .filter(order -> order.status() == OrderStatus.READY_FOR_PROCESSING)
                .findFirst();
        return processable.map(Order::id).orElse(null);
    }
}
