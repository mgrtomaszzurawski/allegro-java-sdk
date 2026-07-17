/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.core;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A monetary amount in a single currency — the SDK-wide money value type.
 *
 * <p>Allegro models every price, fee and refund as a {@code {amount, currency}}
 * pair where {@code amount} is a decimal <em>string</em> (e.g. {@code "10.99"}).
 * The amount is kept as the exact server string so no precision or trailing-zero
 * information is lost in a {@code double} round-trip; use {@link #amountAsDecimal()}
 * for arithmetic. Currency is an ISO-4217 code (e.g. {@code "PLN"}).
 *
 * <p>Shared across every domain bucket (offers, orders, payments, pricing,
 * campaigns) so prices are one type, never re-modelled per feature. Bucket
 * mappers build it from their generated {@code *Raw} DTO via
 * {@link #of(String, String)}.
 *
 * @param amount   decimal amount as the exact string Allegro returned
 * @param currency ISO-4217 currency code
 * @since 0.2.0
 */
public record Money(String amount, String currency) {

    private static final String ERR_AMOUNT = "amount must not be null or blank";
    private static final String ERR_CURRENCY = "currency must not be null or blank";

    /**
     * Canonical constructor — rejects a missing amount or currency so an invalid
     * {@code Money} can never reach the wire.
     */
    public Money {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException(ERR_AMOUNT);
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(ERR_CURRENCY);
        }
    }

    /** Build from an amount string and currency code (the mapper entry point). */
    public static Money of(String amount, String currency) {
        return new Money(amount, currency);
    }

    /** Build from a {@link BigDecimal} amount, rendered as its plain string form. */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(Objects.requireNonNull(amount, ERR_AMOUNT).toPlainString(), currency);
    }

    /** The amount as a {@link BigDecimal} for arithmetic or comparison. */
    public BigDecimal amountAsDecimal() {
        return new BigDecimal(amount);
    }
}
