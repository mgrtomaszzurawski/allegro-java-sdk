/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailableProductResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A product the seller may ship into One Fulfillment. Read lazily via
 * {@code fulfillment().availableProducts()}.
 *
 * @param id     Allegro product identifier
 * @param name   product name
 * @param gtins  GTIN/EAN codes (never {@code null}; empty when none)
 * @param image  product image URL
 *
 * @since 0.3.0
 */
public record AvailableProduct(
        @Nullable String id,
        @Nullable String name,
        List<String> gtins,
        @Nullable String image) {

    public AvailableProduct {
        gtins = gtins == null ? List.of() : List.copyOf(gtins);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static AvailableProduct from(AvailableProductResponseRaw raw) {
        return new AvailableProduct(
                raw.getId() == null ? null : raw.getId().toString(),
                raw.getName(),
                raw.getGtins(),
                raw.getImage());
    }
}
