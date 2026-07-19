/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * Shared mapping from a generated Layer-1 {@code PriceRaw} leaf to the SDK-wide
 * {@link Money}, for the orders-bucket records that carry an optional price.
 */
final class Prices {

    private Prices() {
    }

    /** Money from a price leaf, or {@code null} when the leaf or its parts are absent. */
    static @Nullable Money money(@Nullable PriceRaw price) {
        if (price == null || price.getAmount() == null || price.getCurrency() == null) {
            return null;
        }
        return Money.of(price.getAmount(), price.getCurrency());
    }
}
