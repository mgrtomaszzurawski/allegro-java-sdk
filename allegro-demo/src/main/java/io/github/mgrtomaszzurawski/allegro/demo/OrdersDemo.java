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
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Live sandbox probes for the orders facade (TESTING.md §2, live write→read /
 * shape verification through the SDK).
 *
 * <p>An Allegro order is created by a <em>buyer</em> purchasing — the seller
 * app cannot create one via the API — so the order-keyed probe ({@code orders-get})
 * reads an order the buyer-bot (or a one-time manual buyer purchase) has seeded
 * on the sandbox. The listing probe ({@code orders-list}) needs no seeded order:
 * it verifies the list-response shape (empty is fine) with the seller token alone.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=orders-list
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=orders-get -Pdemo.orderId=&lt;id&gt;
 * </pre>
 *
 * Output is status-level only — never bodies, tokens, or buyer PII.
 */
public final class OrdersDemo {

    private static final String ORDER_ID_PROPERTY = "demo.orderId";
    private static final int LIST_PROBE_LIMIT = 5;
    private static final String NO_ORDER_ID =
            "No -Pdemo.orderId=<id> given. Seed a sandbox order (buyer purchase / buyer-bot), "
                    + "then rerun: -Pdemo.scenario=orders-get -Pdemo.orderId=<id>.";
    private static final String NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first.";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";

    private OrdersDemo() {
    }

    /** {@code orders-get}: read one seeded order back and assert the round-trip id. */
    public static void run(String clientId, String clientSecret, String account) throws IOException {
        String orderId = System.getProperty(ORDER_ID_PROPERTY);
        if (orderId == null || orderId.isBlank()) {
            System.out.println(NO_ORDER_ID);
            return;
        }
        withSellerClient(clientId, clientSecret, account, client -> {
            Order order = client.orders().get(orderId);
            // Status-level output only — no buyer name/email/phone (personal data).
            System.out.println("orders().get(): id=" + order.id()
                    + ", status=" + order.status()
                    + ", sellerStatus=" + order.sellerStatus()
                    + ", lineItems=" + order.lineItems().size()
                    + ", totalToPay=" + order.totalToPay().amount()
                    + " " + order.totalToPay().currency());
            if (!order.id().equals(orderId)) {
                throw new IllegalStateException("Round-trip id mismatch: asked "
                        + orderId + ", got " + order.id());
            }
            System.out.println("Round-trip OK: the order read back matches the requested id.");
        });
    }

    /** {@code orders-list}: verify the list-response shape (no seeded order needed). */
    public static void runList(String clientId, String clientSecret, String account) throws IOException {
        withSellerClient(clientId, clientSecret, account, client -> {
            List<Order> orders = client.orders().streamOrders(OrderFilter.all())
                    .limit(LIST_PROBE_LIMIT)
                    .toList();
            System.out.println("orders().streamOrders(): first " + orders.size()
                    + " order(s) read (limit " + LIST_PROBE_LIMIT + ")");
            for (Order order : orders) {
                // Ids and statuses only — never buyer PII.
                System.out.println("  id=" + order.id()
                        + ", status=" + order.status()
                        + ", sellerStatus=" + order.sellerStatus());
            }
            System.out.println("Shape OK: the orders stream mapped without error.");
        });
    }

    private static void withSellerClient(String clientId, String clientSecret, String account,
            Consumer<AllegroClient> action) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            action.accept(client);
            // Rotation: persist the refreshed token so sibling sessions stay valid.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
        }
    }
}
