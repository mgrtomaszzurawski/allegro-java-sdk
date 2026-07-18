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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Coordinates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
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
        // Self-heal: remove any leftover demo points from an interrupted run.
        for (PointOfService leftover : points.list()) {
            if (leftover.name().startsWith(DEMO_PREFIX)) {
                points.delete(leftover.id());
                System.out.println("cleaned up leftover POS id=" + leftover.id());
            }
        }

        String uniqueSuffix = Long.toString(System.currentTimeMillis());
        // openHours use the ISO HH:mm:ss.SSS time format; coordinates are required
        // on the live create/update (see KNOWN-SERVER-BEHAVIORS.md). The seller id
        // is resolved by the SDK, not set here.
        PointOfServiceRequest request = PointOfServiceRequest.builder()
                .name(DEMO_PREFIX + "Pickup " + uniqueSuffix)
                .type(PosType.PICKUP_POINT)
                .status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.AWAIT_CONTACT)
                .address(Address.builder()
                        .street("Grunwaldzka 100").city("Gdansk").zipCode("80-244")
                        .state("pomorskie").countryCode("PL")
                        .coordinates(new Coordinates(54.372158, 18.638306)).build())
                .openHours(List.of(OpenHour.builder()
                        .dayOfWeek("MONDAY").fromTime("08:00:00.000").toTime("16:00:00.000").build()))
                .externalId(EXTERNAL_ID_PREFIX + uniqueSuffix)
                .build();

        PointOfService created = createOrReport(points, request);
        System.out.println("created POS id=" + created.id() + " status=" + created.status()
                + " confirmationType=" + created.confirmationType());
        try {
            PointOfService readBack = points.get(created.id());
            System.out.println("read-back id=" + readBack.id()
                    + " name-matches=" + readBack.name().equals(request.name())
                    + " openHours=" + readBack.openHours());

            // update: full-representation PUT that renames the point
            PointOfServiceRequest updateRequest = request.toBuilder()
                    .name(request.name() + " EAST").build();
            PointOfService updated = points.update(created.id(), updateRequest);
            System.out.println("updated name=" + updated.name());

            boolean roundTripOk = readBack.id().equals(created.id())
                    && readBack.name().equals(request.name())
                    && readBack.type() == PosType.PICKUP_POINT
                    && updated.name().equals(updateRequest.name());
            if (!roundTripOk) {
                throw new IllegalStateException(ERR_ROUND_TRIP);
            }
        } finally {
            points.delete(created.id());
            System.out.println("deleted POS id=" + created.id());
        }
    }

    /**
     * Create the point of service, printing the server's parsed field errors
     * (code + path + technical message) before rethrowing — so a live 400 names
     * exactly which field the wire rejected, not just that it did.
     */
    private static PointOfService createOrReport(PointsOfService points,
            PointOfServiceRequest request) {
        try {
            return points.create(request);
        } catch (AllegroBadRequestException rejected) {
            System.out.println("create rejected (400/422); field errors:");
            rejected.errors().forEach(fieldError -> System.out.println(
                    "  - code=" + fieldError.code()
                            + " path=" + fieldError.path()
                            + " message=" + fieldError.message()));
            throw rejected;
        }
    }
}
