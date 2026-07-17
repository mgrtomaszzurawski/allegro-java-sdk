/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder.ContactRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model.Contact;
import java.util.List;

/**
 * Seller contact cards — reached via {@code AllegroClient.contacts()}.
 *
 * <p>A contact card is a reusable block of seller contact data (a display name,
 * an optional e-mail, up to two phone numbers) that offers can reference, for
 * example on classified advertisements. This is the starter slice of bucket J
 * (post-sale-comms); the message center and post-purchase issue facades ship
 * alongside it per the task-division plan.
 *
 * <p>Backed by the {@code /sale/offer-contacts} resource; reads need the
 * {@code sale:settings:read} scope and writes need {@code sale:settings:write}.
 *
 * @since 0.2.0
 */
public interface Contacts {

    /**
     * All contact cards defined on the authenticated seller's account. The
     * resource is not paginated — Allegro returns the full set in one response.
     *
     * @return the seller's contact cards; never {@code null}, possibly empty
     */
    List<Contact> list();

    /**
     * A single contact card by its identifier.
     *
     * @param contactId the contact card identifier
     * @return the contact card
     */
    Contact get(String contactId);

    /**
     * Creates a new contact card.
     *
     * @param request the contact data to create
     * @return the created contact card, including its server-assigned id
     */
    Contact create(ContactRequest request);

    /**
     * Replaces the data of an existing contact card.
     *
     * @param contactId the identifier of the contact card to modify
     * @param request the new contact data
     * @return the updated contact card
     */
    Contact update(String contactId, ContactRequest request);
}
