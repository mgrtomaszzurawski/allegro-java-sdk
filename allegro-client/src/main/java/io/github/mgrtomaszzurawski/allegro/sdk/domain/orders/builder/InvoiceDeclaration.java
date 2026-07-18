/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import org.jspecify.annotations.Nullable;

/**
 * Metadata for a new order invoice declared via
 * {@code orders().invoices().declare(...)}, before the file bytes are uploaded.
 * The invoice number and the file name are both required (the file name labels
 * the file uploaded in the second step).
 *
 * @since 0.6.0
 */
public final class InvoiceDeclaration {

    private static final String ERR_INVOICE_NUMBER = "invoiceNumber is required";
    private static final String ERR_FILE_NAME = "fileName is required";

    private final String invoiceNumber;
    private final String fileName;

    private InvoiceDeclaration(Builder builder) {
        this.invoiceNumber = require(builder.invoiceNumber, ERR_INVOICE_NUMBER);
        this.fileName = require(builder.fileName, ERR_FILE_NAME);
    }

    private static String require(@Nullable String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    /** The seller's invoice number. */
    public String invoiceNumber() {
        return invoiceNumber;
    }

    /** The file name to register for the invoice document. */
    public String fileName() {
        return fileName;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this declaration. */
    public Builder toBuilder() {
        return new Builder().invoiceNumber(invoiceNumber).fileName(fileName);
    }

    /** Fluent builder for {@link InvoiceDeclaration}. */
    public static final class Builder {

        private @Nullable String invoiceNumber;
        private @Nullable String fileName;

        /** Set the invoice number (required). */
        public Builder invoiceNumber(@Nullable String value) {
            this.invoiceNumber = value;
            return this;
        }

        /** Set the file name (optional). */
        public Builder fileName(@Nullable String value) {
            this.fileName = value;
            return this;
        }

        /**
         * Build the declaration.
         *
         * @throws IllegalStateException if the invoice number is missing
         */
        public InvoiceDeclaration build() {
            return new InvoiceDeclaration(this);
        }
    }
}
