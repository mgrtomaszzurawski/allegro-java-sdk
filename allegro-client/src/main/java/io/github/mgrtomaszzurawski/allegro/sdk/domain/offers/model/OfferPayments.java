/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The offer's payment settings, reached from {@link Offer#payments()} — the invoice type the
 * seller issues for the offer. The same immutable value is used both ways: read one back from
 * an {@link Offer}, or {@linkplain #of(InvoiceType) build one} to attach to a create request.
 *
 * @param invoice the invoice type the seller provides, or {@code null}
 * @since 0.6.0
 */
public record OfferPayments(
        @Nullable InvoiceType invoice) {

    /** The payment settings declaring the given invoice type for a write. */
    public static OfferPayments of(InvoiceType invoice) {
        return new OfferPayments(Objects.requireNonNull(invoice, "invoice"));
    }

    /** Project a generated payments block onto the consumer value, or {@code null}. */
    public static @Nullable OfferPayments from(@Nullable PaymentsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new OfferPayments(InvoiceType.from(raw.getInvoice()));
    }

    /**
     * The generated request payments block. The {@code invoice} type must be a value a client
     * can request (not {@link InvoiceType#UNKNOWN}).
     *
     * @throws IllegalArgumentException if {@code invoice} is {@link InvoiceType#UNKNOWN}
     */
    public PaymentsRaw toRaw() {
        PaymentsRaw raw = new PaymentsRaw();
        if (invoice != null) {
            raw.invoice(invoice.toRaw());
        }
        return raw;
    }
}
