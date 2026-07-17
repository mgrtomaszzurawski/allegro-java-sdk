/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.contacts;

import io.github.mgrtomaszzurawski.allegro.client.model.ContactRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ContactResponseListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ContactResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.EmailRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PhonesRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.Contacts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder.ContactRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model.Contact;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link Contacts} facade — the four
 * {@code /sale/offer-contacts} operations.
 *
 * @since 0.2.0
 */
public final class ContactsImpl implements Contacts {

    private static final String OP_LIST = "list contacts";
    private static final String OP_GET = "get contact";
    private static final String OP_CREATE = "create contact";
    private static final String OP_UPDATE = "update contact";

    private final HttpSupport http;

    public ContactsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<Contact> list() {
        return Contact.fromList(
                http.getAuthenticated(ApiPaths.OFFER_CONTACTS, ContactResponseListRaw.class, OP_LIST));
    }

    @Override
    public Contact get(String contactId) {
        return Contact.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.OFFER_CONTACTS, contactId), ContactResponseRaw.class, OP_GET));
    }

    @Override
    public Contact create(ContactRequest request) {
        return Contact.from(http.postJsonAuthenticated(
                ApiPaths.OFFER_CONTACTS, toRaw(request), ContactResponseRaw.class, OP_CREATE));
    }

    @Override
    public Contact update(String contactId, ContactRequest request) {
        return Contact.from(http.putJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.OFFER_CONTACTS, contactId), toRaw(request),
                ContactResponseRaw.class, OP_UPDATE));
    }

    private static ContactRequestRaw toRaw(ContactRequest request) {
        // A contact card is a full-state PUT/POST: the request carries the whole
        // desired card. name() may be null (the field is optional); emails/phones
        // are sent as given (an empty list clears that channel on update).
        ContactRequestRaw raw = new ContactRequestRaw().name(request.name());
        for (String address : request.emails()) {
            raw.addEmailsItem(new EmailRequestRaw().address(address));
        }
        for (String number : request.phones()) {
            raw.addPhonesItem(new PhonesRequestRaw().number(number));
        }
        return raw;
    }
}
