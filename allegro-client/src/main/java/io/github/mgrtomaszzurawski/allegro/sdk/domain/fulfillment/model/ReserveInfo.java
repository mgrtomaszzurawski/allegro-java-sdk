/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReserveInfoRaw;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * How comfortable a product's warehouse reserve is: an estimate of how many days
 * until it runs out, and a health {@link ReserveStatus}.
 *
 * @param outOfStockIn estimated days until the product is out of stock
 * @param status       reserve-health classification
 *
 * @since 0.3.0
 */
public record ReserveInfo(
        @Nullable BigDecimal outOfStockIn,
        @Nullable ReserveStatus status) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReserveInfo from(ReserveInfoRaw raw) {
        return new ReserveInfo(
                raw.getOutOfStockIn(),
                raw.getStatus() == null ? null : ReserveStatus.fromWire(raw.getStatus().getValue()));
    }
}
