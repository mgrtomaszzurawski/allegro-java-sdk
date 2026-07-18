/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormBuyerReferenceRaw;
import org.jspecify.annotations.Nullable;

/**
 * The buyer on an order, as seen by the seller.
 *
 * @param id numeric buyer identifier
 * @param login public login name
 * @param email contact e-mail for this order
 * @param firstName first name, or {@code null} when not provided
 * @param lastName last name, or {@code null} when not provided
 * @param companyName company name, or {@code null} for a private buyer
 * @param guest {@code true} when the purchase was made without an Allegro account
 * @param phoneNumber contact phone number, or {@code null} when not provided
 *
 * @since 0.3.0
 */
public record Buyer(
        String id,
        String login,
        String email,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String companyName,
        boolean guest,
        @Nullable String phoneNumber) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static Buyer from(CheckoutFormBuyerReferenceRaw raw) {
        return new Buyer(
                raw.getId(),
                raw.getLogin(),
                raw.getEmail(),
                raw.getFirstName(),
                raw.getLastName(),
                raw.getCompanyName(),
                Boolean.TRUE.equals(raw.getGuest()),
                raw.getPhoneNumber());
    }

    /**
     * Redacts the buyer's personal data (login, e-mail, name, phone) so an
     * accidental log or trace of a {@code Buyer} never leaks it; use the typed
     * accessors to read the fields deliberately.
     */
    @Override
    public String toString() {
        return "Buyer[id=" + id + ", guest=" + guest + ", personal data redacted]";
    }
}
