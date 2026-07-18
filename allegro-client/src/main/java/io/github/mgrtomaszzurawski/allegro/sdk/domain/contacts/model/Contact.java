/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ContactResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ContactResponseListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.EmailResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PhonesResponseRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A seller contact card, as returned by the {@code Contacts} facade.
 *
 * <p>The wire wraps each e-mail and phone in a single-field object; this record
 * flattens them to plain strings, since the address / number is the only datum
 * carried.
 *
 * @param id server-assigned contact identifier
 * @param name display name, or {@code null} when the card has none
 * @param emails e-mail addresses; never {@code null}, possibly empty
 * @param phones phone numbers; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record Contact(
        String id,
        @Nullable String name,
        List<String> emails,
        List<String> phones) {

    public Contact {
        emails = List.copyOf(emails);
        phones = List.copyOf(phones);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Contact from(ContactResponseRaw raw) {
        return new Contact(
                raw.getId(),
                raw.getName(),
                addresses(raw.getEmails()),
                numbers(raw.getPhones()));
    }

    /** Map a full contact-list response to immutable records. */
    public static List<Contact> fromList(ContactResponseListRaw raw) {
        List<ContactResponseRaw> contacts = raw.getContacts();
        if (contacts == null) {
            return List.of();
        }
        return contacts.stream().map(Contact::from).toList();
    }

    private static List<String> addresses(@Nullable List<EmailResponseRaw> emails) {
        if (emails == null) {
            return List.of();
        }
        return emails.stream().map(EmailResponseRaw::getAddress).filter(Objects::nonNull).toList();
    }

    private static List<String> numbers(@Nullable List<PhonesResponseRaw> phones) {
        if (phones == null) {
            return List.of();
        }
        return phones.stream().map(PhonesResponseRaw::getNumber).filter(Objects::nonNull).toList();
    }
}
