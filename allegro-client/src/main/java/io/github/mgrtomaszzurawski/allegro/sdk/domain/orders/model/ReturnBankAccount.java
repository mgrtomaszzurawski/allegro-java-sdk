/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundBankAccountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CustomerReturnRefundBankAccountAddressRaw;
import org.jspecify.annotations.Nullable;

/**
 * The buyer's bank account for a manual refund of a customer return — the seller
 * transfers the refund here when it cannot be settled through Allegro.
 *
 * <p>Highly sensitive personal/financial data: the whole record's {@link #toString()}
 * is redacted (owner, account number, IBAN, SWIFT and address) so an accidental log or
 * trace never leaks it. Read the individual accessors deliberately.
 *
 * @param owner the account owner's name, or {@code null}
 * @param accountNumber the account number, or {@code null}
 * @param iban the IBAN, or {@code null}
 * @param swift the SWIFT/BIC, or {@code null}
 * @param address the account address, or {@code null}
 *
 * @since 0.7.0
 */
public record ReturnBankAccount(
        @Nullable String owner,
        @Nullable String accountNumber,
        @Nullable String iban,
        @Nullable String swift,
        @Nullable ReturnBankAddress address) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnBankAccount from(CustomerReturnRefundBankAccountRaw raw) {
        CustomerReturnRefundBankAccountAddressRaw address = raw.getAddress();
        return new ReturnBankAccount(
                raw.getOwner(),
                raw.getAccountNumber(),
                raw.getIban(),
                raw.getSwift(),
                address == null ? null : ReturnBankAddress.from(address));
    }

    /** Redacts all account fields (sensitive financial data); read accessors deliberately. */
    @Override
    public String toString() {
        return "ReturnBankAccount[redacted]";
    }
}
