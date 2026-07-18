/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.StockQuantityRaw;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * On-hand quantities for a fulfilled product: what can be sold now, what is
 * committed to open orders, and what is held aside.
 *
 * @param available quantity available for sale
 * @param onOrder   quantity committed to orders not yet shipped
 * @param onHold    quantity held aside (e.g. quality checks)
 *
 * @since 0.3.0
 */
public record StockQuantity(
        @Nullable BigDecimal available,
        @Nullable BigDecimal onOrder,
        @Nullable BigDecimal onHold) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static StockQuantity from(StockQuantityRaw raw) {
        return new StockQuantity(raw.getAvailable(), raw.getOnOrder(), raw.getOnHold());
    }
}
