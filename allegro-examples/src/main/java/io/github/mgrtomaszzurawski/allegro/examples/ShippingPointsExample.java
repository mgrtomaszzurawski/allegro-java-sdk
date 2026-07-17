/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Address;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/shipping.md} points-of-service snippet —
 * if the documented code stops compiling, this module breaks the build.
 */
public final class ShippingPointsExample {

    private ShippingPointsExample() {
    }

    static String createAndReadPointOfService(AllegroCredentials credentials) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {

            PointOfService created = client.shipping().points().create(
                    PointOfServiceRequest.builder()
                            .name("Pickup Point Center")
                            .type(PosType.PICKUP_POINT)
                            .status(PosStatus.ACTIVE)
                            .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
                            .address(Address.builder()
                                    .street("Grunwaldzka 100")
                                    .city("Gdansk")
                                    .zipCode("80-244")
                                    .state("pomorskie")
                                    .countryCode("PL")
                                    .build())
                            .openHours(List.of(OpenHour.builder()
                                    .dayOfWeek("MONDAY").fromTime("08:00").toTime("16:00").build()))
                            .externalId("store-001")
                            .build());

            PointOfService readBack = client.shipping().points().get(created.id());
            return readBack.name();
        }
    }
}
