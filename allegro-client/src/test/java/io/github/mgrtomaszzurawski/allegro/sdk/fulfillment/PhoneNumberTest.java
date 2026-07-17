/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import org.junit.jupiter.api.Test;

class PhoneNumberTest {

    private static final int MAX_NUMBER_LENGTH = 15;

    @Test
    void of_whenValid_keepsBothParts() {
        // when
        PhoneNumber phone = PhoneNumber.of("48", "123123123");

        // then
        assertEquals("48", phone.countryCode());
        assertEquals("123123123", phone.number());
    }

    @Test
    void of_whenCountryCodeBlank_throws() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of(" ", "123"));
    }

    @Test
    void of_whenCountryCodeTooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("1234", "123"));
    }

    @Test
    void of_whenNumberBlank_throws() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("48", ""));
    }

    @Test
    void of_whenNumberTooLong_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PhoneNumber.of("48", "1".repeat(MAX_NUMBER_LENGTH + 1)));
    }

    @Test
    void of_whenNumberAtMaxLength_succeeds() {
        PhoneNumber phone = PhoneNumber.of("48", "1".repeat(MAX_NUMBER_LENGTH));
        assertEquals(MAX_NUMBER_LENGTH, phone.number().length());
    }

    @Test
    void toString_doesNotLeakTheNumber() {
        assertFalse(PhoneNumber.of("48", "123123123").toString().contains("123123123"));
    }
}
