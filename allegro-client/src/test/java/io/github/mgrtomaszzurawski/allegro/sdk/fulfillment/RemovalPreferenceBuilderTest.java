/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.WithdrawalAddressBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import org.junit.jupiter.api.Test;

class RemovalPreferenceBuilderTest {

    private static WithdrawalAddress address() {
        return new WithdrawalAddressBuilder()
                .company("Warehouse Sp. z o.o.")
                .street("Uliczna 7")
                .postalCode("60-166")
                .city("Poznan")
                .countryCode("PL")
                .phone(PhoneNumber.of("48", "123123123"))
                .build();
    }

    @Test
    void build_whenOperationOnly_buildsDisposalWithoutAddress() {
        // when
        RemovalPreference preference = RemovalPreference.builder()
                .operation(RemovalOperation.DISPOSAL)
                .build();

        // then
        assertEquals(RemovalOperation.DISPOSAL, preference.operation());
        assertNull(preference.withdrawalAddress());
    }

    @Test
    void build_whenOperationAndAddress_buildsWithdrawal() {
        // given
        WithdrawalAddress address = address();

        // when
        RemovalPreference preference = RemovalPreference.builder()
                .operation(RemovalOperation.WITHDRAWAL)
                .withdrawalAddress(address)
                .build();

        // then
        assertEquals(RemovalOperation.WITHDRAWAL, preference.operation());
        assertSame(address, preference.withdrawalAddress());
    }

    @Test
    void build_whenOperationMissing_throws() {
        var builder = RemovalPreference.builder();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void toBuilder_whenWithdrawal_preservesOperationAndAddress() {
        // given
        RemovalPreference original = RemovalPreference.builder()
                .operation(RemovalOperation.WITHDRAWAL)
                .withdrawalAddress(address())
                .build();

        // when
        RemovalPreference copy = original.toBuilder().build();

        // then
        assertEquals(original.operation(), copy.operation());
        assertEquals(original.withdrawalAddress(), copy.withdrawalAddress());
    }

    @Test
    void toBuilder_whenDisposal_preservesOperationAndNullAddress() {
        // given
        RemovalPreference original = RemovalPreference.builder()
                .operation(RemovalOperation.DISPOSAL)
                .build();

        // when
        RemovalPreference copy = original.toBuilder().build();

        // then
        assertEquals(RemovalOperation.DISPOSAL, copy.operation());
        assertNull(copy.withdrawalAddress());
    }
}
