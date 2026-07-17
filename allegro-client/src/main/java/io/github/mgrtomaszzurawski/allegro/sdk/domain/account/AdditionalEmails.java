/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.AdditionalEmail;
import java.util.List;

/**
 * Additional e-mail addresses registered on the authenticated account —
 * reached via {@code AllegroClient.user().additionalEmails()}. Reads need the
 * {@code profile:read} scope; {@link #add} and {@link #delete} need
 * {@code profile:write}.
 *
 * @since 0.2.0
 */
public interface AdditionalEmails {

    /**
     * All additional e-mail addresses on the account.
     *
     * @return the addresses; never {@code null}, possibly empty
     */
    List<AdditionalEmail> list();

    /**
     * A single additional e-mail address by id.
     *
     * @param emailId identifier of the entry
     * @return the address entry
     */
    AdditionalEmail get(String emailId);

    /**
     * Add a new additional e-mail address to the account.
     *
     * @param emailAddress a valid e-mail address to add
     * @return the created entry, with its server-assigned id
     */
    AdditionalEmail add(String emailAddress);

    /**
     * Remove an additional e-mail address from the account.
     *
     * @param emailId identifier of the entry to delete
     */
    void delete(String emailId);
}
