/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReceiverAddressDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SenderAddressDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.PostalAddressBuilder;
import org.jspecify.annotations.Nullable;

/**
 * A postal contact address used as a shipment's sender or receiver. Allegro
 * models both ends with the same fields, so one record serves both roles: the
 * street, postal code, city, e-mail and phone are always present; the personal
 * or company name, the state and the pickup-point id are optional.
 *
 * @param name the addressee's name, or {@code null}
 * @param company the company name, or {@code null}
 * @param street the street and building/flat number
 * @param postalCode the postal code
 * @param city the city
 * @param state the region/voivodeship, or {@code null}
 * @param email the contact e-mail
 * @param phone the contact phone number
 * @param point the pickup/drop-off point id for point-based methods, or {@code null}
 *
 * @since 0.4.0
 */
public record PostalAddress(
        @Nullable String name,
        @Nullable String company,
        String street,
        String postalCode,
        String city,
        @Nullable String state,
        String email,
        String phone,
        @Nullable String point) {

    /** A fresh builder for a {@link PostalAddress}. */
    public static PostalAddressBuilder builder() {
        return new PostalAddressBuilder();
    }

    /** A builder pre-loaded with this address's fields. */
    public PostalAddressBuilder toBuilder() {
        return new PostalAddressBuilder()
                .name(name)
                .company(company)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .state(state)
                .email(email)
                .phone(phone)
                .point(point);
    }

    /** Map a sender address DTO to the public record (spec-required fields trusted). */
    public static PostalAddress fromSender(SenderAddressDtoRaw raw) {
        return new PostalAddress(raw.getName(), raw.getCompany(), raw.getStreet(),
                raw.getPostalCode(), raw.getCity(), raw.getState(), raw.getEmail(),
                raw.getPhone(), raw.getPoint());
    }

    /** Map a receiver address DTO to the public record (spec-required fields trusted). */
    public static PostalAddress fromReceiver(ReceiverAddressDtoRaw raw) {
        return new PostalAddress(raw.getName(), raw.getCompany(), raw.getStreet(),
                raw.getPostalCode(), raw.getCity(), raw.getState(), raw.getEmail(),
                raw.getPhone(), raw.getPoint());
    }

    /** Build the sender-address DTO for a request body. */
    public SenderAddressDtoRaw toSenderRaw() {
        SenderAddressDtoRaw raw = new SenderAddressDtoRaw();
        raw.setName(name);
        raw.setCompany(company);
        raw.setStreet(street);
        raw.setPostalCode(postalCode);
        raw.setCity(city);
        raw.setState(state);
        raw.setEmail(email);
        raw.setPhone(phone);
        raw.setPoint(point);
        return raw;
    }

    /** Build the receiver-address DTO for a request body. */
    public ReceiverAddressDtoRaw toReceiverRaw() {
        ReceiverAddressDtoRaw raw = new ReceiverAddressDtoRaw();
        raw.setName(name);
        raw.setCompany(company);
        raw.setStreet(street);
        raw.setPostalCode(postalCode);
        raw.setCity(city);
        raw.setState(state);
        raw.setEmail(email);
        raw.setPhone(phone);
        raw.setPoint(point);
        return raw;
    }
}
