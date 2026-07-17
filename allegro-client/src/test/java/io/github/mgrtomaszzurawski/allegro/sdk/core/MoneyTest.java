/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final String AMOUNT = "10.99";
    private static final String CURRENCY = "PLN";

    @Test
    void of_whenAmountAndCurrency_keepsExactServerString() {
        // when
        Money money = Money.of(AMOUNT, CURRENCY);

        // then — the exact string is preserved (no double round-trip)
        assertEquals(AMOUNT, money.amount());
        assertEquals(CURRENCY, money.currency());
    }

    @Test
    void ofBigDecimal_whenTrailingZeros_rendersPlainString() {
        // when — 10.90 must not collapse to 10.9 via scientific notation
        Money money = Money.of(new BigDecimal("10.90"), CURRENCY);

        // then
        assertEquals("10.90", money.amount());
    }

    @Test
    void ofBigDecimal_whenAmountNull_rejectsFailFastWithSameExceptionAsStringOverload() {
        // then — both factories reject a missing amount the same way (IAE, not NPE)
        assertThrows(IllegalArgumentException.class, () -> Money.of((BigDecimal) null, CURRENCY));
    }

    @Test
    void amountAsDecimal_whenParsed_equalsOriginal() {
        // then
        assertEquals(new BigDecimal(AMOUNT), Money.of(AMOUNT, CURRENCY).amountAsDecimal());
    }

    @Test
    void constructor_whenAmountBlank_rejectsFailFast() {
        // then
        assertThrows(IllegalArgumentException.class, () -> Money.of("  ", CURRENCY));
        assertThrows(IllegalArgumentException.class, () -> Money.of((String) null, CURRENCY));
    }

    @Test
    void constructor_whenCurrencyBlank_rejectsFailFast() {
        // then
        assertThrows(IllegalArgumentException.class, () -> Money.of(AMOUNT, ""));
        assertThrows(IllegalArgumentException.class, () -> Money.of(AMOUNT, null));
    }
}
