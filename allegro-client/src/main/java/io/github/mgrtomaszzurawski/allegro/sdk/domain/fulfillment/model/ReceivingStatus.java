/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReceivingStatusRaw;
import org.jspecify.annotations.Nullable;

/**
 * A quantity of a product received in one disposition during unpacking: how many
 * units, the {@link ReceivedType} they were sorted into, and the {@link ReasonCode}
 * behind that sorting.
 *
 * @param quantity     how many units this line covers, when reported
 * @param receivedType the disposition assigned to the units, when reported
 * @param reasonCode   the reason behind the disposition, when reported
 *
 * @since 0.4.0
 */
public record ReceivingStatus(
        @Nullable Integer quantity,
        @Nullable ReceivedType receivedType,
        @Nullable ReasonCode reasonCode) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReceivingStatus from(ReceivingStatusRaw raw) {
        return new ReceivingStatus(
                raw.getQuantity(),
                raw.getReceivedType() == null ? null : ReceivedType.fromWire(raw.getReceivedType().getValue()),
                raw.getReasonCode() == null ? null : ReasonCode.fromWire(raw.getReasonCode().getValue()));
    }
}
