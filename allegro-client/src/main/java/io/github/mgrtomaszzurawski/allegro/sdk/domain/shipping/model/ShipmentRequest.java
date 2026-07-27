/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.InsuranceDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentCreateRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShipmentRequestBuilder;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The request to create a carrier shipment. The sender, the receiver and at
 * least one package are required; the carrier credentials, the reference number,
 * insurance, cash-on-delivery and label format are optional (the request is
 * typically taken from a delivery proposal's suggested input).
 *
 * @param credentialsId the carrier-credentials id to use, or {@code null}
 * @param sender the sender address
 * @param receiver the receiver address
 * @param referenceNumber the seller reference number, or {@code null}
 * @param packages the parcels to ship (at least one)
 * @param insurance the declared insurance amount, or {@code null}
 * @param cashOnDelivery the cash-on-delivery instruction, or {@code null}
 * @param labelFormat the requested label file format, or {@code null} for the carrier default
 *
 * @since 0.4.0
 */
public record ShipmentRequest(
        @Nullable String credentialsId,
        PostalAddress sender,
        PostalAddress receiver,
        @Nullable String referenceNumber,
        List<ShipmentPackage> packages,
        @Nullable Money insurance,
        @Nullable CashOnDelivery cashOnDelivery,
        @Nullable LabelFormat labelFormat) {

    /** Canonical constructor: defensively copy the package list. */
    public ShipmentRequest {
        packages = List.copyOf(packages);
    }

    /** A fresh builder for a {@link ShipmentRequest}. */
    public static ShipmentRequestBuilder builder() {
        return new ShipmentRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public ShipmentRequestBuilder toBuilder() {
        return new ShipmentRequestBuilder()
                .credentialsId(credentialsId)
                .sender(sender)
                .receiver(receiver)
                .referenceNumber(referenceNumber)
                .packages(packages)
                .insurance(insurance)
                .cashOnDelivery(cashOnDelivery)
                .labelFormat(labelFormat);
    }

    /**
     * Reconstruct a request from the generated DTO echoed as a delivery
     * proposal's suggested input, so the consumer receives a ready-to-submit
     * {@link ShipmentRequest} they can adjust and pass to {@code createShipment}.
     * The sender, receiver and packages are required by the spec on this echo.
     *
     * <p>{@code credentialsId} is deprecated in the spec (the merchant-agreement
     * WZA flow), but the field is still carried on the echo and surfaced by this
     * record, so it is read faithfully for round-trip fidelity — see
     * {@link #credentialsId(ShipmentCreateRequestDtoRaw)}.
     */
    public static ShipmentRequest from(ShipmentCreateRequestDtoRaw raw) {
        return new ShipmentRequest(
                credentialsId(raw),
                PostalAddress.fromSender(raw.getSender()),
                PostalAddress.fromReceiver(raw.getReceiver()),
                raw.getReferenceNumber(),
                raw.getPackages() == null
                        ? List.of()
                        : raw.getPackages().stream().map(ShipmentPackage::fromRequest).toList(),
                insurance(raw),
                CashOnDelivery.from(raw.getCashOnDelivery()),
                raw.getLabelFormat() == null
                        ? null
                        : LabelFormat.fromWire(raw.getLabelFormat().getValue()));
    }

    /**
     * Read the deprecated {@code credentialsId} field off the echo. Isolated so
     * the deprecation suppression stays confined to the single deprecated call.
     */
    @SuppressWarnings("deprecation")
    private static @Nullable String credentialsId(ShipmentCreateRequestDtoRaw raw) {
        return raw.getCredentialsId();
    }

    private static @Nullable Money insurance(ShipmentCreateRequestDtoRaw raw) {
        InsuranceDtoRaw insuranceRaw = raw.getInsurance();
        if (insuranceRaw == null
                || insuranceRaw.getAmount() == null
                || insuranceRaw.getCurrency() == null) {
            return null;
        }
        return Money.of(insuranceRaw.getAmount(), insuranceRaw.getCurrency());
    }

    /** Build the generated Layer-1 DTO for the create-command request body. */
    public ShipmentCreateRequestDtoRaw toRaw() {
        ShipmentCreateRequestDtoRaw raw = new ShipmentCreateRequestDtoRaw();
        raw.setCredentialsId(credentialsId);
        raw.setSender(sender.toSenderRaw());
        raw.setReceiver(receiver.toReceiverRaw());
        raw.setReferenceNumber(referenceNumber);
        raw.setPackages(packages.stream().map(ShipmentPackage::toRaw).toList());
        if (insurance != null) {
            InsuranceDtoRaw insuranceRaw = new InsuranceDtoRaw();
            insuranceRaw.setAmount(insurance.amount());
            insuranceRaw.setCurrency(insurance.currency());
            raw.setInsurance(insuranceRaw);
        }
        if (cashOnDelivery != null) {
            raw.setCashOnDelivery(cashOnDelivery.toRaw());
        }
        if (labelFormat != null) {
            raw.setLabelFormat(
                    ShipmentCreateRequestDtoRaw.LabelFormatEnum.fromValue(labelFormat.wireValue()));
        }
        return raw;
    }
}
