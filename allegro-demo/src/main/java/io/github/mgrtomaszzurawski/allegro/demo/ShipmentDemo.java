/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.Shipping;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Shipment;
import java.io.IOException;
import java.util.List;

/**
 * Bucket C live probe (TESTING.md §2) for the shipment-management read and
 * render paths. Creating a shipment needs a real seeded order behind the WZA
 * broker (buyer buy-now is CAPTCHA-blocked from this datacenter IP — see the
 * PR-2c note in {@code BACKLOG.md}), so this probe verifies the read side only:
 * given a shipment id, it reads the shipment back through the SDK and renders its
 * label and handover protocol, printing status-level counts (never bytes, PII or
 * tokens).
 *
 * <p>Pass the id of a shipment on the seller account:
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=shipment -Pdemo.account=seller \
 *       -Ddemo.shipmentId=&lt;shipmentId&gt;
 * </pre>
 *
 * With no id the probe explains what it needs and exits cleanly, so it never
 * fails the run before an order has been seeded.
 */
public final class ShipmentDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "shipment";

    private static final String SHIPMENT_ID_PROPERTY = "demo.shipmentId";
    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String NO_SHIPMENT_ID =
            "No -Ddemo.shipmentId set - seed a shipment on the seller account first "
                    + "(needs a real order behind the WZA broker), then rerun with the id";

    private ShipmentDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        String shipmentId = System.getProperty(SHIPMENT_ID_PROPERTY);
        if (shipmentId == null || shipmentId.isBlank()) {
            System.out.println(NO_SHIPMENT_ID);
            return;
        }
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
                probe(client.shipping(), shipmentId);
            } finally {
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void probe(Shipping shipping, String shipmentId) {
        Shipment shipment = shipping.getShipment(shipmentId);
        System.out.println("getShipment(): id=" + shipment.id()
                + ", carrier=" + shipment.carrier()
                + ", labelFormat=" + shipment.labelFormat()
                + ", packages=" + shipment.packages().size()
                + ", canceled=" + (shipment.canceledDate() != null));

        byte[] label = shipping.labels(LabelRequest.builder()
                .shipmentIds(List.of(shipmentId))
                .build());
        System.out.println("labels(): " + label.length + " bytes");

        byte[] protocol = shipping.protocol(shipmentId);
        System.out.println("protocol(): " + protocol.length + " bytes");
    }
}
