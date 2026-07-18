/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRefundDispositionProductRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The product involved in a {@link RefundDisposition}.
 *
 * @param gtins    GTIN/EAN codes (never {@code null}; empty when none)
 * @param name     product name
 * @param quantity number of units in this disposition
 *
 * @since 0.3.0
 */
public record RefundProduct(
        List<String> gtins,
        @Nullable String name,
        @Nullable Integer quantity) {

    public RefundProduct {
        gtins = gtins == null ? List.of() : List.copyOf(gtins);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static RefundProduct from(FulfillmentRefundDispositionProductRaw raw) {
        return new RefundProduct(raw.getGtins(), raw.getName(), raw.getQuantity());
    }
}
