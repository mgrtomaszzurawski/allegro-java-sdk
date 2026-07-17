/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OpenHourRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PaymentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PosLocationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PosRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A seller's point of service (personal pickup location), as returned by
 * {@code shipping.points().create(...)} and {@code shipping.points().get(...)}.
 *
 * @param id server-assigned identifier (UUID)
 * @param externalId seller's own identifier from an external system, or {@code null}
 * @param name display name
 * @param sellerId owning seller's id, or {@code null} when the server omits it
 * @param type point type
 * @param address postal address
 * @param phoneNumber contact phone, or {@code null}
 * @param email contact e-mail, or {@code null}
 * @param locationIds server-side location ids (assigned by Allegro); never {@code null}
 * @param openHours opening hours per day; never {@code null}, possibly empty
 * @param serviceTime collection time period (ISO-8601 duration, e.g. {@code "PT24H"}), or {@code null}
 * @param payments accepted payment methods; never {@code null}, possibly empty
 * @param confirmationType how collection is confirmed
 * @param status operational status
 * @param createdAt creation timestamp (ISO-8601 string), or {@code null}
 * @param updatedAt last-modification timestamp (ISO-8601 string), or {@code null}
 *
 * @since 0.2.0
 */
public record PointOfService(
        String id,
        @Nullable String externalId,
        String name,
        @Nullable String sellerId,
        PosType type,
        Address address,
        @Nullable String phoneNumber,
        @Nullable String email,
        List<String> locationIds,
        List<OpenHour> openHours,
        @Nullable String serviceTime,
        List<String> payments,
        ConfirmationType confirmationType,
        PosStatus status,
        @Nullable String createdAt,
        @Nullable String updatedAt) {

    public PointOfService {
        locationIds = List.copyOf(locationIds);
        openHours = List.copyOf(openHours);
        payments = List.copyOf(payments);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static PointOfService from(PosRaw raw) {
        return new PointOfService(
                raw.getId(),
                raw.getExternalId(),
                raw.getName(),
                raw.getSeller() == null ? null : raw.getSeller().getId(),
                PosType.fromWire(raw.getType()),
                Address.from(raw.getAddress()),
                raw.getPhoneNumber(),
                raw.getEmail(),
                locationIds(raw.getLocations()),
                openHours(raw.getOpenHours()),
                raw.getServiceTime(),
                payments(raw.getPayments()),
                ConfirmationType.fromWire(raw.getConfirmationType()),
                PosStatus.fromWire(raw.getStatus()),
                raw.getCreatedAt(),
                raw.getUpdatedAt());
    }

    private static List<String> locationIds(@Nullable List<PosLocationRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(PosLocationRaw::getId).toList();
    }

    private static List<OpenHour> openHours(@Nullable List<OpenHourRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(OpenHour::from).toList();
    }

    private static List<String> payments(@Nullable List<PaymentRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(PaymentRaw::getMethod).toList();
    }
}
