/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PosRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.PointOfServiceRequestBuilder;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A request to create a point of service, assembled with
 * {@link #builder()}. The {@code name}, {@code type}, {@code status},
 * {@code confirmationType} and {@code address} fields are required and validated
 * fail-fast by the builder; {@code openHours} is always sent (possibly empty).
 *
 * @param name display name (required, max 80 chars)
 * @param type point type (required)
 * @param status operational status (required)
 * @param confirmationType how collection is confirmed (required)
 * @param address postal address (required)
 * @param openHours opening hours; never {@code null}, possibly empty
 * @param externalId seller's own identifier, or {@code null} (max 80 chars)
 * @param phoneNumber contact phone, or {@code null} (max 16 chars)
 * @param email contact e-mail, or {@code null} (max 64 chars)
 * @param serviceTime collection time period (ISO-8601 duration), or {@code null}
 *
 * @since 0.2.0
 */
public record PointOfServiceRequest(
        String name,
        PosType type,
        PosStatus status,
        ConfirmationType confirmationType,
        Address address,
        List<OpenHour> openHours,
        @Nullable String externalId,
        @Nullable String phoneNumber,
        @Nullable String email,
        @Nullable String serviceTime) {

    public PointOfServiceRequest {
        openHours = List.copyOf(openHours);
    }

    /** A fresh builder for a {@link PointOfServiceRequest}. */
    public static PointOfServiceRequestBuilder builder() {
        return new PointOfServiceRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public PointOfServiceRequestBuilder toBuilder() {
        return new PointOfServiceRequestBuilder()
                .name(name)
                .type(type)
                .status(status)
                .confirmationType(confirmationType)
                .address(address)
                .openHours(openHours)
                .externalId(externalId)
                .phoneNumber(phoneNumber)
                .email(email)
                .serviceTime(serviceTime);
    }

    /** Build the generated Layer-1 DTO for the request body. */
    public PosRaw toRaw() {
        PosRaw raw = new PosRaw();
        raw.setName(name);
        raw.setType(type.wireValue());
        raw.setStatus(status.wireValue());
        raw.setConfirmationType(confirmationType.wireValue());
        raw.setAddress(address.toRaw());
        raw.setOpenHours(openHours.stream().map(OpenHour::toRaw).toList());
        if (externalId != null) {
            raw.setExternalId(externalId);
        }
        if (phoneNumber != null) {
            raw.setPhoneNumber(phoneNumber);
        }
        if (email != null) {
            raw.setEmail(email);
        }
        if (serviceTime != null) {
            raw.setServiceTime(serviceTime);
        }
        return raw;
    }
}
