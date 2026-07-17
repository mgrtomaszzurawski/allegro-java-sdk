/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.PointsOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Address;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import java.io.IOException;
import java.util.List;

/**
 * Sandbox write→read verification for the points-of-service facade (bucket C
 * starter slice): create a point of service through the SDK, read it back and
 * assert the round-trip, then delete it (self-cleaning). Confirms the mapping
 * and builder against the live wire, not just against WireMock stubs
 * (TESTING.md §2). Status-level output only — never bodies or tokens.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=pos-roundtrip -Pdemo.account=seller
 * </pre>
 */
public final class PointsOfServiceDemo {

    private static final String DEMO_PREFIX = "[C-demo] ";
    private static final String EXTERNAL_ID_PREFIX = "c-demo-";
    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_ROUND_TRIP = "write->read mismatch: created and read-back differ";

    private PointsOfServiceDemo() {
    }

    /** Scenario entry point matching {@link DemoScenario}. */
    public static void run(String clientId, String clientSecret, String account) throws IOException {
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
                roundTrip(client.shipping().points());
            } finally {
                // Rotation: the refresh just performed invalidated the stored
                // token, so persist the new one for the next run and siblings.
                String rotatedRefreshToken = client.refreshToken();
                if (rotatedRefreshToken != null) {
                    tokenStore.store(account, rotatedRefreshToken);
                }
            }
        }
    }

    private static void roundTrip(PointsOfService points) {
        String uniqueSuffix = Long.toString(System.currentTimeMillis());
        PointOfServiceRequest request = PointOfServiceRequest.builder()
                .name(DEMO_PREFIX + "Pickup " + uniqueSuffix)
                .type(PosType.PICKUP_POINT)
                .status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
                .address(Address.builder()
                        .street("Grunwaldzka 100").city("Gdansk").zipCode("80-244")
                        .state("pomorskie").countryCode("PL").build())
                .openHours(List.of(OpenHour.builder()
                        .dayOfWeek("MONDAY").fromTime("08:00").toTime("16:00").build()))
                .externalId(EXTERNAL_ID_PREFIX + uniqueSuffix)
                .build();

        PointOfService created = points.create(request);
        System.out.println("created POS id=" + created.id() + " status=" + created.status());

        PointOfService readBack = points.get(created.id());
        boolean roundTripOk = readBack.id().equals(created.id())
                && readBack.name().equals(request.name())
                && readBack.type() == PosType.PICKUP_POINT;
        System.out.println("read-back id=" + readBack.id()
                + " name-matches=" + readBack.name().equals(request.name())
                + " type=" + readBack.type());

        points.delete(created.id());
        System.out.println("deleted POS id=" + created.id());

        if (!roundTripOk) {
            throw new IllegalStateException(ERR_ROUND_TRIP);
        }
    }
}
