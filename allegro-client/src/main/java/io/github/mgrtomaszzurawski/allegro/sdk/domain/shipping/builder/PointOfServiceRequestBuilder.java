/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Address;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link PointOfServiceRequest}. {@code name},
 * {@code type}, {@code status}, {@code confirmationType} and {@code address} are
 * required; {@code openHours} defaults to an empty list (always sent, since the
 * server requires the field present). Length limits mirror the Allegro contract.
 *
 * @since 0.2.0
 */
public final class PointOfServiceRequestBuilder {

    private static final int MAX_NAME = 80;
    private static final int MAX_EXTERNAL_ID = 80;
    private static final int MAX_PHONE_NUMBER = 16;
    private static final int MAX_EMAIL = 64;

    private static final String FIELD_NAME = "PointOfServiceRequest.name";
    private static final String FIELD_TYPE = "PointOfServiceRequest.type";
    private static final String FIELD_STATUS = "PointOfServiceRequest.status";
    private static final String FIELD_CONFIRMATION_TYPE = "PointOfServiceRequest.confirmationType";
    private static final String FIELD_ADDRESS = "PointOfServiceRequest.address";
    private static final String FIELD_EXTERNAL_ID = "PointOfServiceRequest.externalId";
    private static final String FIELD_PHONE_NUMBER = "PointOfServiceRequest.phoneNumber";
    private static final String FIELD_EMAIL = "PointOfServiceRequest.email";

    private @Nullable String name;
    private @Nullable PosType type;
    private @Nullable PosStatus status;
    private @Nullable ConfirmationType confirmationType;
    private @Nullable Address address;
    private List<OpenHour> openHours = List.of();
    private @Nullable String externalId;
    private @Nullable String phoneNumber;
    private @Nullable String email;
    private @Nullable String serviceTime;

    /** Display name (required, max 80 chars). */
    public PointOfServiceRequestBuilder name(@Nullable String value) {
        this.name = value;
        return this;
    }

    /** Point type (required). */
    public PointOfServiceRequestBuilder type(@Nullable PosType value) {
        this.type = value;
        return this;
    }

    /** Operational status (required). */
    public PointOfServiceRequestBuilder status(@Nullable PosStatus value) {
        this.status = value;
        return this;
    }

    /** How collection is confirmed (required). */
    public PointOfServiceRequestBuilder confirmationType(@Nullable ConfirmationType value) {
        this.confirmationType = value;
        return this;
    }

    /** Postal address (required). */
    public PointOfServiceRequestBuilder address(@Nullable Address value) {
        this.address = value;
        return this;
    }

    /** Opening hours (optional; an empty list is always sent when omitted). */
    public PointOfServiceRequestBuilder openHours(@Nullable List<OpenHour> value) {
        this.openHours = value == null ? List.of() : List.copyOf(value);
        return this;
    }

    /** Seller's own identifier (optional, max 80 chars). */
    public PointOfServiceRequestBuilder externalId(@Nullable String value) {
        this.externalId = value;
        return this;
    }

    /** Contact phone (optional, max 16 chars). */
    public PointOfServiceRequestBuilder phoneNumber(@Nullable String value) {
        this.phoneNumber = value;
        return this;
    }

    /** Contact e-mail (optional, max 64 chars). */
    public PointOfServiceRequestBuilder email(@Nullable String value) {
        this.email = value;
        return this;
    }

    /** Collection time period as an ISO-8601 duration, e.g. {@code "PT24H"} (optional). */
    public PointOfServiceRequestBuilder serviceTime(@Nullable String value) {
        this.serviceTime = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link PointOfServiceRequest}.
     *
     * @throws IllegalStateException if a required field is missing or a length
     *     limit is exceeded
     */
    public PointOfServiceRequest build() {
        String validName = BuilderValidation.requireText(name, FIELD_NAME);
        PosType validType = BuilderValidation.requirePresent(type, FIELD_TYPE);
        PosStatus validStatus = BuilderValidation.requirePresent(status, FIELD_STATUS);
        ConfirmationType validConfirmationType =
                BuilderValidation.requirePresent(confirmationType, FIELD_CONFIRMATION_TYPE);
        Address validAddress = BuilderValidation.requirePresent(address, FIELD_ADDRESS);
        BuilderValidation.requireMaxLength(validName, MAX_NAME, FIELD_NAME);
        BuilderValidation.requireMaxLength(externalId, MAX_EXTERNAL_ID, FIELD_EXTERNAL_ID);
        BuilderValidation.requireMaxLength(phoneNumber, MAX_PHONE_NUMBER, FIELD_PHONE_NUMBER);
        BuilderValidation.requireMaxLength(email, MAX_EMAIL, FIELD_EMAIL);
        return new PointOfServiceRequest(validName, validType, validStatus, validConfirmationType,
                validAddress, openHours, externalId, phoneNumber, email, serviceTime);
    }
}
