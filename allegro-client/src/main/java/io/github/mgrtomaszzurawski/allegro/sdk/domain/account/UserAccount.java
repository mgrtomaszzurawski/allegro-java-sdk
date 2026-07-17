/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;

/**
 * Account and user information — reached via {@code AllegroClient.user()}.
 *
 * <p>Root facade of bucket D's user domain: the everyday account operations
 * ({@link #me()}) live here, and coherent tool-sets hang off sub-accessors
 * ({@link #additionalEmails()}). Every operation needs a user-context token
 * (authorization-code or device grant); an app-only client-credentials token
 * is limited to public data.
 *
 * @since 0.1.0
 */
public interface UserAccount {

    /**
     * Basic information about the authenticated user (requires a user-context
     * token; an app-only client-credentials token is rejected by Allegro).
     *
     * @return the authenticated user's profile
     */
    CurrentUser me();

    /**
     * Additional e-mail addresses registered on the account.
     *
     * @return the additional-emails sub-facade
     */
    AdditionalEmails additionalEmails();
}
