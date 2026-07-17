/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.Contacts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder.ContactRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model.Contact;
import java.io.IOException;
import java.util.Optional;

/**
 * Bucket J write→read verification for the contacts facade (TESTING.md §2):
 * create-or-update the demo contact card THROUGH the SDK, then read it back and
 * assert the round-trip. The {@code /sale/offer-contacts} resource has no delete
 * operation, so the scenario reuses a single {@code [J-demo]} card instead of
 * accumulating one per run — the write becomes an update once the card exists.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=contacts -Pdemo.account=seller
 * </pre>
 */
public final class ContactsDemo {

    private static final String DEMO_CONTACT_NAME = "[J-demo] contact";
    private static final String DEMO_EMAIL = "j-demo@example.com";
    private static final String DEMO_PHONE = "+48512000000";
    private static final String MSG_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private ContactsDemo() {
    }

    /** Entry point registered in {@link DemoApp}'s scenario table. */
    public static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_EXPIRED), storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            Contact readBack = writeThenRead(client.contacts());
            // Rotation: the refresh we just did invalidated the stored token.
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
            boolean roundTrip = DEMO_CONTACT_NAME.equals(readBack.name())
                    && readBack.emails().contains(DEMO_EMAIL)
                    && readBack.phones().contains(DEMO_PHONE);
            System.out.println("contacts write->read: id=" + readBack.id()
                    + ", name=" + readBack.name()
                    + ", emails=" + readBack.emails().size()
                    + ", phones=" + readBack.phones().size()
                    + ", roundTrip=" + roundTrip);
        }
    }

    private static Contact writeThenRead(Contacts contacts) {
        ContactRequest request = ContactRequest.builder()
                .name(DEMO_CONTACT_NAME)
                .email(DEMO_EMAIL)
                .phone(DEMO_PHONE)
                .build();
        Optional<Contact> existing = contacts.list().stream()
                .filter(contact -> DEMO_CONTACT_NAME.equals(contact.name()))
                .findFirst();
        Contact written = existing
                .map(contact -> contacts.update(contact.id(), request))
                .orElseGet(() -> contacts.create(request));
        return contacts.get(written.id());
    }
}
