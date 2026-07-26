/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnItemReasonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One item within a {@link CustomerReturn} — what the buyer returned, in what
 * quantity, at what price, and why.
 *
 * @param offerId the returned offer's id, or {@code null}
 * @param quantity the returned quantity, or {@code null}
 * @param name the offer name at the time of return, or {@code null}
 * @param price the item price, or {@code null}
 * @param url the offer url, or {@code null}
 * @param reason why the item is being returned, or {@code null}
 * @param serialNumbers the returned item's serial numbers; empty when none
 *
 * @since 0.7.0
 */
public record ReturnedItem(
        @Nullable String offerId,
        @Nullable Long quantity,
        @Nullable String name,
        @Nullable Money price,
        @Nullable String url,
        @Nullable ReturnItemReason reason,
        List<String> serialNumbers) {

    /** Defensive, null-tolerant copy of the serial-numbers list. */
    public ReturnedItem {
        serialNumbers = serialNumbers == null ? List.of() : List.copyOf(serialNumbers);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnedItem from(CustomerReturnItemRaw raw) {
        PriceRaw price = raw.getPrice();
        CustomerReturnItemReasonRaw reason = raw.getReason();
        return new ReturnedItem(
                raw.getOfferId(),
                raw.getQuantity(),
                raw.getName(),
                price == null ? null : Money.of(price.getAmount(), price.getCurrency()),
                raw.getUrl(),
                reason == null ? null : ReturnItemReason.from(reason),
                raw.getSerialNumbers() == null ? List.of()
                        : raw.getSerialNumbers().stream().filter(Objects::nonNull).toList());
    }
}
