/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemTaxRaw;
import org.jspecify.annotations.Nullable;

/**
 * The tax applied to a {@link LineItem}: the VAT rate, the tax subject, and any
 * exemption reason.
 *
 * @param rate the VAT rate (e.g. {@code "23"}), or {@code null} when not set
 * @param subject the tax subject, or {@code null} when not set
 * @param exemption the exemption reason, or {@code null} when the item is not exempt
 *
 * @since 0.8.0
 */
public record LineItemTax(
        @Nullable String rate,
        @Nullable String subject,
        @Nullable String exemption) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable LineItemTax from(@Nullable CheckoutFormLineItemTaxRaw raw) {
        if (raw == null) {
            return null;
        }
        return new LineItemTax(raw.getRate(), raw.getSubject(), raw.getExemption());
    }
}
