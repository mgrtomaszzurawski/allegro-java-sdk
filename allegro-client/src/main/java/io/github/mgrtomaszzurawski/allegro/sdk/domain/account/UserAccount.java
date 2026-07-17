/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;

/**
 * Account and user information — reached via {@code AllegroClient.user()}.
 *
 * <p>Bootstrap slice of bucket D (account-meta): only {@link #me()} ships with
 * the core-runtime PR as the end-to-end proof of the auth/transport stack. The
 * bucket owner extends this facade per the task-division plan.
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
}
