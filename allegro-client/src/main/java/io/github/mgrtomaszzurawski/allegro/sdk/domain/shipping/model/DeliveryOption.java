/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryOptionDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One way a delivery proposal can be fulfilled: how the parcel reaches the buyer
 * ({@link DeliveryType}), when the carrier is paid ({@link DeliveryPayment}), the
 * package kind, the origin and destination countries, the carrier limits and the
 * optional additional services and properties. Read-only.
 *
 * @param deliveryType how the parcel is delivered
 * @param paymentType when the carrier is paid
 * @param packageType the package kind this option handles
 * @param originCountry the origin country code, or {@code null}
 * @param destinationCountry the destination country code, or {@code null}
 * @param limits the carrier limits for this option, or {@code null}
 * @param additionalServices the optional carrier services on offer
 * @param additionalProperties the carrier-specific properties on offer
 *
 * @since 0.5.0
 */
public record DeliveryOption(
        DeliveryType deliveryType,
        DeliveryPayment paymentType,
        PackageType packageType,
        @Nullable String originCountry,
        @Nullable String destinationCountry,
        @Nullable DeliveryLimits limits,
        List<AdditionalService> additionalServices,
        List<AdditionalProperty> additionalProperties) {

    /** Canonical constructor: defensively copy the lists. */
    public DeliveryOption {
        additionalServices = List.copyOf(additionalServices);
        additionalProperties = List.copyOf(additionalProperties);
    }

    /** Map the generated DTO. */
    public static DeliveryOption from(DeliveryOptionDtoRaw raw) {
        return new DeliveryOption(
                DeliveryType.fromWire(deliveryWire(raw)),
                DeliveryPayment.fromWire(paymentWire(raw)),
                PackageType.fromWire(packageWire(raw)),
                raw.getOriginCountry(),
                raw.getDestinationCountry(),
                DeliveryLimits.from(raw.getLimits()),
                mapServices(raw),
                mapProperties(raw));
    }

    private static @Nullable String deliveryWire(DeliveryOptionDtoRaw raw) {
        return raw.getDeliveryType() == null ? null : raw.getDeliveryType().getValue();
    }

    private static @Nullable String paymentWire(DeliveryOptionDtoRaw raw) {
        return raw.getPaymentType() == null ? null : raw.getPaymentType().getValue();
    }

    private static @Nullable String packageWire(DeliveryOptionDtoRaw raw) {
        return raw.getPackageType() == null ? null : raw.getPackageType().getValue();
    }

    private static List<AdditionalService> mapServices(DeliveryOptionDtoRaw raw) {
        return raw.getAdditionalServices() == null
                ? List.of()
                : raw.getAdditionalServices().stream().map(AdditionalService::from).toList();
    }

    private static List<AdditionalProperty> mapProperties(DeliveryOptionDtoRaw raw) {
        return raw.getAdditionalProperties() == null
                ? List.of()
                : raw.getAdditionalProperties().stream().map(AdditionalProperty::from).toList();
    }
}
