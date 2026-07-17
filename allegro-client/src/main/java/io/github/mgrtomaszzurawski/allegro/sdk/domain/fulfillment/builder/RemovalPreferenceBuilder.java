/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link RemovalPreference}. {@code operation} is required;
 * a {@code withdrawalAddress} is optional here — Allegro requires it for a
 * {@link RemovalOperation#WITHDRAWAL} and rejects a preference that omits it, so
 * that constraint is enforced by the server (a typed error), not fabricated by
 * the SDK.
 *
 * @since 0.2.0
 */
public final class RemovalPreferenceBuilder {

    private static final String ERR_OPERATION = "operation is required";

    private @Nullable RemovalOperation operation;
    private @Nullable WithdrawalAddress withdrawalAddress;

    /** What to do with removable goods (required). */
    public RemovalPreferenceBuilder operation(RemovalOperation operation) {
        this.operation = operation;
        return this;
    }

    /** Return address for a {@link RemovalOperation#WITHDRAWAL}. */
    public RemovalPreferenceBuilder withdrawalAddress(WithdrawalAddress withdrawalAddress) {
        this.withdrawalAddress = withdrawalAddress;
        return this;
    }

    /**
     * Validate and assemble the preference.
     *
     * @throws IllegalStateException if {@code operation} was not set
     */
    public RemovalPreference build() {
        if (operation == null) {
            throw new IllegalStateException(ERR_OPERATION);
        }
        return new RemovalPreference(operation, withdrawalAddress);
    }
}
