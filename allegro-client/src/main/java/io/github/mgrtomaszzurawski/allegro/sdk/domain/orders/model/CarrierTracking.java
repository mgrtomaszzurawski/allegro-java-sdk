/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CarrierParcelTrackingResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParcelTrackingHistoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParcelTrackingHistoryTrackingDetailsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParcelTrackingStatusRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Carrier-reported delivery history for one or more waybills of an order.
 *
 * <p>Status {@link TrackingStatus#code()} values come straight from the carrier
 * and are exposed as raw strings rather than a fixed enum: the set of codes is
 * carrier-specific and open-ended, so a typed enum would reject unseen values.
 *
 * @param carrierId identifier of the carrier, or {@code null} when absent
 * @param waybills per-waybill tracking histories; never {@code null}
 *
 * @since 0.4.0
 */
public record CarrierTracking(@Nullable String carrierId, List<TrackedWaybill> waybills) {

    public CarrierTracking {
        waybills = List.copyOf(waybills);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static CarrierTracking from(CarrierParcelTrackingResponseRaw raw) {
        List<ParcelTrackingHistoryRaw> rawWaybills = raw.getWaybills();
        List<TrackedWaybill> waybills = rawWaybills == null
                ? List.of()
                : rawWaybills.stream().map(TrackedWaybill::from).toList();
        return new CarrierTracking(raw.getCarrierId(), waybills);
    }

    /**
     * Delivery history for a single waybill.
     *
     * @param waybill the carrier's tracking (waybill) number
     * @param statuses ordered status history; never {@code null}
     * @param updatedAt when the carrier last updated this history, or {@code null}
     */
    public record TrackedWaybill(
            String waybill,
            List<TrackingStatus> statuses,
            @Nullable OffsetDateTime updatedAt) {

        public TrackedWaybill {
            statuses = List.copyOf(statuses);
        }

        static TrackedWaybill from(ParcelTrackingHistoryRaw raw) {
            ParcelTrackingHistoryTrackingDetailsRaw details = raw.getTrackingDetails();
            List<ParcelTrackingStatusRaw> rawStatuses = details == null ? null : details.getStatuses();
            List<TrackingStatus> statuses = rawStatuses == null
                    ? List.of()
                    : rawStatuses.stream().map(TrackingStatus::from).toList();
            return new TrackedWaybill(
                    raw.getWaybill(),
                    statuses,
                    details == null ? null : details.getUpdatedAt());
        }
    }

    /**
     * One status in a waybill's delivery history.
     *
     * @param code carrier status code (raw carrier value), or {@code null}
     * @param description human-readable status text, or {@code null}
     * @param occurredAt when the carrier recorded this status, or {@code null}
     */
    public record TrackingStatus(
            @Nullable String code,
            @Nullable String description,
            @Nullable OffsetDateTime occurredAt) {

        static TrackingStatus from(ParcelTrackingStatusRaw raw) {
            var code = raw.getCode();
            return new TrackingStatus(
                    code == null ? null : code.getValue(),
                    raw.getDescription(),
                    raw.getOccurredAt());
        }
    }
}
