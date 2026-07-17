/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import java.io.IOException;

/**
 * Live sandbox probe for the orders facade starter slice: read one order back
 * through the SDK and assert the response shape (TESTING.md §2, read-only
 * shape verification).
 *
 * <p>An Allegro order is created by a <em>buyer</em> purchasing — the seller
 * app cannot create one via the API — so this scenario reads an order the
 * buyer-bot (or a one-time manual buyer purchase) has seeded on the sandbox.
 * Pass its id:
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=orders-get -Pdemo.orderId=&lt;id&gt;
 * </pre>
 *
 * With no {@code demo.orderId} the scenario prints what is needed and exits
 * without failing — the seeded order is an external dependency, not a code bug.
 * Output is status-level only — never bodies, tokens, or buyer PII.
 */
public final class OrdersDemo {

    private static final String ORDER_ID_PROPERTY = "demo.orderId";
    private static final String NO_ORDER_ID =
            "No -Pdemo.orderId=<id> given. Seed a sandbox order (buyer purchase / buyer-bot), "
                    + "then rerun: -Pdemo.scenario=orders-get -Pdemo.orderId=<id>.";
    private static final String NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first.";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";

    private OrdersDemo() {
    }

    /** Entry point registered in {@link DemoApp}. */
    public static void run(String clientId, String clientSecret, String account) throws IOException {
        String orderId = System.getProperty(ORDER_ID_PROPERTY);
        if (orderId == null || orderId.isBlank()) {
            System.out.println(NO_ORDER_ID);
            return;
        }
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
            Order order = client.orders().get(orderId);
            // Rotation: persist the refreshed token so sibling sessions stay valid.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
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
        }
    }
}
