/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsView;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.JoinStrategy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelPageSize;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Shipment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Weight;
import java.math.BigDecimal;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/shipping.md} delivery-settings and
 * shipping-rates snippets — if the documented code stops compiling, this module
 * breaks the build.
 */
public final class ShippingConfigExample {

    private ShippingConfigExample() {
    }

    static JoinStrategy readAndUpdateDeliverySettings(AllegroCredentials credentials) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {

            DeliverySettingsView current = client.shipping().settings().get();

            DeliverySettingsView updated = client.shipping().settings().update(
                    DeliverySettingsRequest.builder()
                            .joinPolicy(JoinStrategy.SUM)
                            .freeDelivery(Money.of("200.00", "PLN"))
                            .abroadFreeDelivery(Money.of("500.00", "PLN"))
                            .build());

            return updated.joinPolicy() == null ? current.joinPolicy() : updated.joinPolicy();
        }
    }

    static String createShippingRateSet(AllegroCredentials credentials, String deliveryMethodId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {

            List<ShippingRateSetSummary> existing = client.shipping().rates().list();
            if (!existing.isEmpty()) {
                ShippingRateSet firstSet = client.shipping().rates().get(existing.get(0).id());
                return firstSet.name();
            }

            ShippingRateSet created = client.shipping().rates().create(
                    ShippingRateSetRequest.builder()
                            .name("Domestic rates")
                            .type(RateSetType.PHYSICAL)
                            .dispatchCountry("PL")
                            .rates(List.of(
                                    ShippingRate.builder()
                                            .deliveryMethodId(deliveryMethodId)
                                            .firstItemRate(Money.of("12.99", "PLN"))
                                            .nextItemRate(Money.of("2.00", "PLN"))
                                            .maxQuantityPerPackage(10)
                                            .maxPackageWeight(new Weight("30.0", "KILOGRAMS"))
                                            .build()))
                            .build());

            return created.id();
        }
    }

    static byte[] createShipmentAndRenderLabel(AllegroCredentials credentials, String credentialsId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {

            Shipment shipment = client.shipping().createShipment(
                    ShipmentRequest.builder()
                            .credentialsId(credentialsId)
                            .sender(PostalAddress.builder()
                                    .street("Grunwaldzka 100").postalCode("80-244").city("Gdansk")
                                    .email("sender@example.com").phone("+48500100100").build())
                            .receiver(PostalAddress.builder()
                                    .street("Marszalkowska 1").postalCode("00-001").city("Warszawa")
                                    .email("receiver@example.com").phone("+48500200200").build())
                            .packages(List.of(ShipmentPackage.builder()
                                    .type(PackageType.PACKAGE)
                                    .lengthCm(new BigDecimal("30")).widthCm(new BigDecimal("20"))
                                    .heightCm(new BigDecimal("10")).weightKg(new BigDecimal("2.5"))
                                    .build()))
                            .labelFormat(LabelFormat.PDF)
                            .build());

            Shipment fetched = client.shipping().getShipment(shipment.id());

            return client.shipping().labels(LabelRequest.builder()
                    .shipmentIds(List.of(fetched.id()))
                    .pageSize(LabelPageSize.A4)
                    .build());
        }
    }
}
