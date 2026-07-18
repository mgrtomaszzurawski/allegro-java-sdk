/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder.ContactRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model.Contact;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/contacts.md} snippets — if the documented
 * contacts usage stops compiling, this module breaks the build.
 */
public final class ContactsExample {

    private ContactsExample() {
    }

    static Contact createContact(AllegroCredentials credentials) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            ContactRequest request = ContactRequest.builder()
                    .name("Main contact")
                    .email("shop@example.com")
                    .phone("+48512323495")
                    .build();
            return client.contacts().create(request);
        }
    }

    static Contact renameContact(AllegroCredentials credentials, String contactId, String newName) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            // An update replaces the whole card, so carry the existing channels over.
            Contact current = client.contacts().get(contactId);
            ContactRequest.Builder request = ContactRequest.builder().name(newName);
            current.emails().forEach(request::email);
            current.phones().forEach(request::phone);
            return client.contacts().update(contactId, request.build());
        }
    }

    static List<Contact> listContacts(AllegroCredentials credentials) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            return client.contacts().list();
        }
    }
}
