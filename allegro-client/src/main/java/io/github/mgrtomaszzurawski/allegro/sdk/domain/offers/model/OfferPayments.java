/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsRaw;
import org.jspecify.annotations.Nullable;

/**
 * The offer's payment settings, reached from {@link Offer#payments()} — chiefly the invoice
 * type the seller issues for the offer.
 *
 * @param invoice the invoice type the seller provides ({@code VAT}/{@code WITHOUT_VAT}/… as
 *                reported by Allegro), or {@code null}
 * @since 0.6.0
 */
public record OfferPayments(
        @Nullable String invoice) {

    /** Project a generated payments block onto the consumer value, or {@code null}. */
    public static @Nullable OfferPayments from(@Nullable PaymentsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new OfferPayments(raw.getInvoice() == null ? null : raw.getInvoice().getValue());
    }
}
