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
