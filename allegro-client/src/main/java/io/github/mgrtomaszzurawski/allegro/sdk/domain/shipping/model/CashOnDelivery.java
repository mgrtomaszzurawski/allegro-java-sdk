/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CashOnDeliveryDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.CashOnDeliveryBuilder;
import org.jspecify.annotations.Nullable;

/**
 * The cash-on-delivery instruction for a shipment: the amount the carrier
 * collects from the receiver and the bank account it is paid into.
 *
 * @param amount the amount to collect
 * @param ownerName the collecting account owner's name, or {@code null}
 * @param iban the IBAN the collected amount is transferred to, or {@code null}
 *
 * @since 0.4.0
 */
public record CashOnDelivery(
        Money amount,
        @Nullable String ownerName,
        @Nullable String iban) {

    /** A fresh builder for a {@link CashOnDelivery}. */
    public static CashOnDeliveryBuilder builder() {
        return new CashOnDeliveryBuilder();
    }

    /** A builder pre-loaded with this instruction's fields. */
    public CashOnDeliveryBuilder toBuilder() {
        return new CashOnDeliveryBuilder()
                .amount(amount)
                .ownerName(ownerName)
                .iban(iban);
    }

    /**
     * Map the generated DTO to the public record, or {@code null} if the shipment
     * carries no cash-on-delivery instruction.
     */
    public static @Nullable CashOnDelivery from(@Nullable CashOnDeliveryDtoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new CashOnDelivery(Money.of(raw.getAmount(), raw.getCurrency()),
                raw.getOwnerName(), raw.getIban());
    }

    /** Build the generated DTO for a request body. */
    public CashOnDeliveryDtoRaw toRaw() {
        CashOnDeliveryDtoRaw raw = new CashOnDeliveryDtoRaw();
        raw.setAmount(amount.amount());
        raw.setCurrency(amount.currency());
        raw.setOwnerName(ownerName);
        raw.setIban(iban);
        return raw;
    }
}
