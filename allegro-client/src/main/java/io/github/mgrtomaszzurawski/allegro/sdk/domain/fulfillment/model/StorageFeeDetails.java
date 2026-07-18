/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.StockStorageFeeDetailsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Detail of a long-term storage fee charged for a fulfilled product: how many
 * items were charged and the net/gross amounts (shared {@link Money} type).
 *
 * @param chargedItemsQuantity number of items the fee was charged for
 * @param netAmount            net fee amount, or {@code null} if not provided
 * @param grossAmount          gross fee amount, or {@code null} if not provided
 *
 * @since 0.3.0
 */
public record StorageFeeDetails(
        @Nullable BigDecimal chargedItemsQuantity,
        @Nullable Money netAmount,
        @Nullable Money grossAmount) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static StorageFeeDetails from(StockStorageFeeDetailsRaw raw) {
        String currency = raw.getCurrency();
        Money netAmount = raw.getAmountNet() != null && currency != null
                ? Money.of(raw.getAmountNet(), currency)
                : null;
        Money grossAmount = raw.getAmountGross() != null && currency != null
                ? Money.of(raw.getAmountGross(), currency)
                : null;
        return new StorageFeeDetails(raw.getChargedItemsQuantity(), netAmount, grossAmount);
    }
}
