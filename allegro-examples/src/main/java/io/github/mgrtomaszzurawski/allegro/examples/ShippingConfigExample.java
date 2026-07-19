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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Weight;
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
}
