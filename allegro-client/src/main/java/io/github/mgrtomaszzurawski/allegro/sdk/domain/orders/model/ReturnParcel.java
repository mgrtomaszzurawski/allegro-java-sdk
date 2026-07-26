/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnParcelSenderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnReturnParcelRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One return parcel — how the returned goods travel back to the seller.
 *
 * <p>{@link #senderPhoneNumber()} is personal data and is redacted from
 * {@link #toString()}; read it deliberately.
 *
 * @param createdAt when the parcel was created, or {@code null}
 * @param waybill the waybill number, or {@code null}
 * @param transportingWaybill the transporting waybill, or {@code null}
 * @param carrierId the carrier id, or {@code null}
 * @param transportingCarrierId the transporting carrier id, or {@code null}
 * @param senderPhoneNumber the sender's phone number (personal data), or {@code null}
 *
 * @since 0.7.0
 */
public record ReturnParcel(
        @Nullable OffsetDateTime createdAt,
        @Nullable String waybill,
        @Nullable String transportingWaybill,
        @Nullable String carrierId,
        @Nullable String transportingCarrierId,
        @Nullable String senderPhoneNumber) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnParcel from(CustomerReturnReturnParcelRaw raw) {
        CustomerReturnParcelSenderRaw sender = raw.getSender();
        return new ReturnParcel(
                raw.getCreatedAt(),
                raw.getWaybill(),
                raw.getTransportingWaybill(),
                raw.getCarrierId(),
                raw.getTransportingCarrierId(),
                sender == null ? null : sender.getPhoneNumber());
    }

    /** Redacts the sender phone number (personal data); logistics fields are kept. */
    @Override
    public String toString() {
        return "ReturnParcel[createdAt=" + createdAt + ", waybill=" + waybill
                + ", transportingWaybill=" + transportingWaybill + ", carrierId=" + carrierId
                + ", transportingCarrierId=" + transportingCarrierId
                + ", sender phone redacted]";
    }
}
