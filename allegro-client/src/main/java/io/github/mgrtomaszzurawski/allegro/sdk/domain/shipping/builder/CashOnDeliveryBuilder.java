/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.CashOnDelivery;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link CashOnDelivery} instruction. The amount is
 * required; the collecting account owner and IBAN are optional.
 *
 * @since 0.4.0
 */
public final class CashOnDeliveryBuilder {

    private static final String FIELD_AMOUNT = "CashOnDelivery.amount";

    private @Nullable Money amount;
    private @Nullable String ownerName;
    private @Nullable String iban;

    /** The amount the carrier collects (required). */
    public CashOnDeliveryBuilder amount(@Nullable Money value) {
        this.amount = value;
        return this;
    }

    /** The collecting account owner's name (optional). */
    public CashOnDeliveryBuilder ownerName(@Nullable String value) {
        this.ownerName = value;
        return this;
    }

    /** The IBAN the collected amount is transferred to (optional). */
    public CashOnDeliveryBuilder iban(@Nullable String value) {
        this.iban = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link CashOnDelivery}.
     *
     * @throws IllegalStateException if the amount is missing
     */
    public CashOnDelivery build() {
        Money validAmount = BuilderValidation.requirePresent(amount, FIELD_AMOUNT);
        return new CashOnDelivery(validAmount, ownerName, iban);
    }
}
