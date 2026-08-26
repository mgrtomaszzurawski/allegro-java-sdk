/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.LineItemDiscountRaw;
import org.jspecify.annotations.Nullable;

/**
 * A discount applied to a {@link LineItem}. Only the discount {@code type} carries
 * a generated accessor; the wire {@code value} is not surfaced by the models layer.
 *
 * @param type the discount type (wire value), or {@code null} when not set
 *
 * @since 0.8.0
 */
public record LineItemDiscount(@Nullable String type) {

    /** Map the generated Layer-1 DTO. */
    public static LineItemDiscount from(LineItemDiscountRaw raw) {
        return new LineItemDiscount(raw.getType() == null ? null : raw.getType().getValue());
    }
}
