/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import org.jspecify.annotations.Nullable;

/**
 * Metadata for a new order invoice declared via
 * {@code orders().invoices().declare(...)}, before the file bytes are uploaded.
 * The file name (which labels the file uploaded in the second step) is required;
 * the invoice number is optional — matching the Allegro spec, which marks the
 * file required and the number optional.
 *
 * @since 0.6.0
 */
public final class InvoiceDeclaration {

    private static final String ERR_FILE_NAME = "fileName is required";

    private final @Nullable String invoiceNumber;
    private final String fileName;

    private InvoiceDeclaration(Builder builder) {
        if (builder.fileName == null || builder.fileName.isBlank()) {
            throw new IllegalStateException(ERR_FILE_NAME);
        }
        this.fileName = builder.fileName;
        this.invoiceNumber = builder.invoiceNumber;
    }

    /** The seller's invoice number, or {@code null} when not provided. */
    public @Nullable String invoiceNumber() {
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

        /** Set the invoice number (optional). */
        public Builder invoiceNumber(@Nullable String value) {
            this.invoiceNumber = value;
            return this;
        }

        /** Set the file name (required). */
        public Builder fileName(@Nullable String value) {
            this.fileName = value;
            return this;
        }

        /**
         * Build the declaration.
         *
         * @throws IllegalStateException if the file name is missing
         */
        public InvoiceDeclaration build() {
            return new InvoiceDeclaration(this);
        }
    }
}
