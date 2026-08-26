/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.InsuranceDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PackageDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReceiverAddressDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SenderAddressDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A carrier shipment created through "Wysyłam z Allegro" (WZA) — the sender and
 * receiver, the parcels, the insurance and cash-on-delivery instructions, and
 * the carrier's label format. A shipment starts without a {@code canceledDate};
 * once cancelled that timestamp is set.
 *
 * @param id the shipment id
 * @param credentialsId the carrier-credentials id used, or {@code null}
 * @param sender the sender address, or {@code null}
 * @param receiver the receiver address, or {@code null}
 * @param referenceNumber the seller reference number, or {@code null}
 * @param packages the parcels in the shipment, possibly empty
 * @param insurance the declared insurance amount, or {@code null}
 * @param cashOnDelivery the cash-on-delivery instruction, or {@code null}
 * @param createdDate when the shipment was created (ISO-8601), or {@code null}
 * @param canceledDate when the shipment was cancelled (ISO-8601), or {@code null}
 * @param carrier the carrier that fulfils the shipment, or {@code null}
 * @param labelFormat the label file format
 * @param pickupAvailable whether a pickup can be requested for it, or {@code null}
 * @param deliveryMethodId the delivery method the shipment uses, or {@code null}
 * @param additionalServices the ids of the additional carrier services requested;
 *     never {@code null}, possibly empty
 * @param transport the transport markers assigned to the shipment; never
 *     {@code null}, possibly empty
 * @param additionalProperties extra carrier-specific key/value properties; never
 *     {@code null}, possibly empty
 *
 * @since 0.4.0
 */
public record Shipment(
        @Nullable String id,
        @Nullable String credentialsId,
        @Nullable PostalAddress sender,
        @Nullable PostalAddress receiver,
        @Nullable String referenceNumber,
        List<ShipmentPackage> packages,
        @Nullable Money insurance,
        @Nullable CashOnDelivery cashOnDelivery,
        @Nullable String createdDate,
        @Nullable String canceledDate,
        @Nullable String carrier,
        LabelFormat labelFormat,
        @Nullable Boolean pickupAvailable,
        @Nullable String deliveryMethodId,
        List<String> additionalServices,
        List<String> transport,
        Map<String, String> additionalProperties) {

    /** Canonical constructor: defensively copy the collections. */
    public Shipment {
        packages = List.copyOf(packages);
        additionalServices = additionalServices == null ? List.of() : List.copyOf(additionalServices);
        transport = transport == null ? List.of() : List.copyOf(transport);
        additionalProperties = additionalProperties == null ? Map.of() : Map.copyOf(additionalProperties);
    }

    /** Map the generated response DTO to the public record. */
    public static Shipment from(ShipmentDtoRaw raw) {
        return new Shipment(
                raw.getId(),
                raw.getCredentialsId(),
                sender(raw.getSender()),
                receiver(raw.getReceiver()),
                raw.getReferenceNumber(),
                packages(raw.getPackages()),
                insurance(raw.getInsurance()),
                CashOnDelivery.from(raw.getCashOnDelivery()),
                raw.getCreatedDate(),
                raw.getCanceledDate(),
                raw.getCarrier(),
                LabelFormat.fromWire(raw.getLabelFormat() == null ? null : raw.getLabelFormat().getValue()),
                raw.getPickupAvailable(),
                raw.getDeliveryMethodId(),
                raw.getAdditionalServices(),
                raw.getTransport(),
                raw.getAdditionalProperties());
    }

    private static @Nullable PostalAddress sender(@Nullable SenderAddressDtoRaw raw) {
        return raw == null ? null : PostalAddress.fromSender(raw);
    }

    private static @Nullable PostalAddress receiver(@Nullable ReceiverAddressDtoRaw raw) {
        return raw == null ? null : PostalAddress.fromReceiver(raw);
    }

    private static List<ShipmentPackage> packages(@Nullable List<PackageDtoRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(ShipmentPackage::from).toList();
    }

    private static @Nullable Money insurance(@Nullable InsuranceDtoRaw raw) {
        return raw == null ? null : Money.of(raw.getAmount(), raw.getCurrency());
    }
}
