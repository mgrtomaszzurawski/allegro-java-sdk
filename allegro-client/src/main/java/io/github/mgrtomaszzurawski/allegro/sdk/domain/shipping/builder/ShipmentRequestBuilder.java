/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.CashOnDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link ShipmentRequest}. The sender, receiver and at
 * least one package are required; every other field is optional.
 *
 * @since 0.4.0
 */
public final class ShipmentRequestBuilder {

    private static final String FIELD_SENDER = "ShipmentRequest.sender";
    private static final String FIELD_RECEIVER = "ShipmentRequest.receiver";
    private static final String FIELD_PACKAGES = "ShipmentRequest.packages";

    private @Nullable String credentialsId;
    private @Nullable PostalAddress sender;
    private @Nullable PostalAddress receiver;
    private @Nullable String referenceNumber;
    private @Nullable List<ShipmentPackage> packages;
    private @Nullable Money insurance;
    private @Nullable CashOnDelivery cashOnDelivery;
    private @Nullable LabelFormat labelFormat;

    /** The carrier-credentials id to use (optional). */
    public ShipmentRequestBuilder credentialsId(@Nullable String value) {
        this.credentialsId = value;
        return this;
    }

    /** The sender address (required). */
    public ShipmentRequestBuilder sender(@Nullable PostalAddress value) {
        this.sender = value;
        return this;
    }

    /** The receiver address (required). */
    public ShipmentRequestBuilder receiver(@Nullable PostalAddress value) {
        this.receiver = value;
        return this;
    }

    /** The seller reference number (optional). */
    public ShipmentRequestBuilder referenceNumber(@Nullable String value) {
        this.referenceNumber = value;
        return this;
    }

    /** The parcels to ship (required; at least one). A defensive copy is taken. */
    public ShipmentRequestBuilder packages(@Nullable List<ShipmentPackage> value) {
        this.packages = value == null ? null : List.copyOf(value);
        return this;
    }

    /** The declared insurance amount (optional). */
    public ShipmentRequestBuilder insurance(@Nullable Money value) {
        this.insurance = value;
        return this;
    }

    /** The cash-on-delivery instruction (optional). */
    public ShipmentRequestBuilder cashOnDelivery(@Nullable CashOnDelivery value) {
        this.cashOnDelivery = value;
        return this;
    }

    /** The requested label file format (optional). */
    public ShipmentRequestBuilder labelFormat(@Nullable LabelFormat value) {
        this.labelFormat = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link ShipmentRequest}.
     *
     * @throws IllegalStateException if the sender, receiver or packages are missing
     */
    public ShipmentRequest build() {
        PostalAddress validSender = BuilderValidation.requirePresent(sender, FIELD_SENDER);
        PostalAddress validReceiver = BuilderValidation.requirePresent(receiver, FIELD_RECEIVER);
        List<ShipmentPackage> validPackages =
                BuilderValidation.requireNonEmpty(packages, FIELD_PACKAGES);
        return new ShipmentRequest(credentialsId, validSender, validReceiver,
                referenceNumber, validPackages, insurance, cashOnDelivery, labelFormat);
    }
}
