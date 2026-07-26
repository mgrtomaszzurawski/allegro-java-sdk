/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import org.jspecify.annotations.Nullable;

/**
 * A change to an offer's payment settings, applied in bulk by
 * {@code offers().batch().modify(...)}: the {@link InvoiceType invoice type} the
 * seller issues, the VAT rate, or both. On the wire this is a single
 * {@code payments} modification element, so — like every other
 * {@link BatchModificationRequest} change — it counts as one field change.
 *
 * <p>At least one of the invoice type or the VAT rate must be set; a
 * modification that changes neither is rejected fail-fast at {@link Builder#build()}.
 *
 * @since 0.6.0
 */
public final class PaymentsModification {

    private static final String ERR_EMPTY =
            "a payments modification must set the invoice type, the VAT rate, or both";
    private static final String ERR_VAT_RATE = "VAT rate must not be null or blank";

    private final @Nullable InvoiceType invoiceType;
    private final @Nullable String vatRate;

    private PaymentsModification(Builder builder) {
        this.invoiceType = builder.invoiceType;
        this.vatRate = builder.vatRate;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The invoice type to set, or {@code null} if the invoice type is unchanged. */
    public @Nullable InvoiceType invoiceType() {
        return invoiceType;
    }

    /** The VAT rate to set (e.g. {@code "23"}), or {@code null} if the rate is unchanged. */
    public @Nullable String vatRate() {
        return vatRate;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private @Nullable InvoiceType invoiceType;
        private @Nullable String vatRate;

        private Builder() {
        }

        /** Set the invoice type the seller issues. */
        public Builder invoiceType(InvoiceType type) {
            this.invoiceType = type;
            return this;
        }

        /** Set the VAT rate (a percentage such as {@code "23"} or {@code "5.5"}). */
        public Builder vatRate(String rate) {
            if (rate == null || rate.isBlank()) {
                throw new IllegalArgumentException(ERR_VAT_RATE);
            }
            this.vatRate = rate;
            return this;
        }

        /** Build, requiring at least one of the invoice type or the VAT rate. */
        public PaymentsModification build() {
            if (invoiceType == null && vatRate == null) {
                throw new IllegalStateException(ERR_EMPTY);
            }
            return new PaymentsModification(this);
        }
    }
}
