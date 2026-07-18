/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.StockStorageFeeRaw;
import org.jspecify.annotations.Nullable;

/**
 * Whether a long-term storage fee applies to a fulfilled product, since when,
 * and (if charged) its {@link StorageFeeDetails}.
 *
 * @param status      whether the fee applies
 * @param feeStatusAt the moment the current fee status took effect (ISO-8601 string)
 * @param details     fee breakdown, present when {@link StorageFeeStatus#CHARGED}
 *
 * @since 0.3.0
 */
public record StockStorageFee(
        @Nullable StorageFeeStatus status,
        @Nullable String feeStatusAt,
        @Nullable StorageFeeDetails details) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static StockStorageFee from(StockStorageFeeRaw raw) {
        return new StockStorageFee(
                raw.getStatus() == null ? null : StorageFeeStatus.fromWire(raw.getStatus().getValue()),
                raw.getFeeStatusAt(),
                raw.getDetails() == null ? null : StorageFeeDetails.from(raw.getDetails()));
    }
}
